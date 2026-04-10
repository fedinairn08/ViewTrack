package com.viewTrack.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viewTrack.config.KinopoiskProperties;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.dto.response.ExternalReviewResponseDto;
import com.viewTrack.service.ExternalReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class KinopoiskExternalReviewServiceImpl implements ExternalReviewService {

    private static final String PROVIDER_NAME = "Кинопоиск";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(7);

    private final KinopoiskProperties kinopoiskProperties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECTION_TIMEOUT)
            .build();

    @Override
    public List<ExternalReviewResponseDto> getReviewsForMovie(Movie movie) {
        if (!isConfigured() || movie == null || movie.getTitle() == null || movie.getTitle().isBlank()) {
            return List.of();
        }

        try {
            KinopoiskMovieDto matchedMovie = findBestMatch(movie);
            if (matchedMovie == null || matchedMovie.id() == null) {
                return List.of();
            }

            return loadReviews(matchedMovie.id());
        } catch (Exception e) {
            log.warn("Не удалось загрузить отзывы из {} для фильма '{}': {}", PROVIDER_NAME, movie.getTitle(), e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean isConfigured() {
        String apiKey = kinopoiskProperties.getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private KinopoiskMovieDto findBestMatch(Movie movie) throws IOException, InterruptedException {
        URI uri = UriComponentsBuilder.fromUriString(kinopoiskProperties.getBaseUrl())
                .pathSegment("v1.4", "movie", "search")
                .queryParam("query", movie.getTitle())
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .build()
                .encode()
                .toUri();

        KinopoiskMovieSearchResponse response = sendRequest(uri, KinopoiskMovieSearchResponse.class);
        if (response == null || response.docs() == null || response.docs().isEmpty()) {
            return null;
        }

        Integer releaseYear = movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null;
        String normalizedTitle = normalize(movie.getTitle());

        return response.docs().stream()
                .max(Comparator.comparingInt(result -> scoreResult(result, normalizedTitle, releaseYear)))
                .orElse(response.docs().get(0));
    }

    private List<ExternalReviewResponseDto> loadReviews(Long kinopoiskMovieId) throws IOException, InterruptedException {
        int maxReviews = Math.max(kinopoiskProperties.getMaxReviews(), 1);

        URI uri = UriComponentsBuilder.fromUriString(kinopoiskProperties.getBaseUrl())
                .pathSegment("v1.4", "review")
                .queryParam("movieId", kinopoiskMovieId)
                .queryParam("page", 1)
                .queryParam("limit", maxReviews)
                .build()
                .encode()
                .toUri();

        KinopoiskReviewResponse response = sendRequest(uri, KinopoiskReviewResponse.class);
        if (response == null || response.docs() == null || response.docs().isEmpty()) {
            return List.of();
        }

        return response.docs().stream()
                .map(this::toExternalReview)
                .filter(review -> review.getContent() != null && !review.getContent().isBlank())
                .sorted(Comparator.comparing(
                        ExternalReviewResponseDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(maxReviews)
                .toList();
    }

    private <T> T sendRequest(URI uri, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(API_KEY_HEADER, kinopoiskProperties.getApiKey())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("{} вернул статус {}", PROVIDER_NAME, response.statusCode());
            return null;
        }

        return objectMapper.readValue(response.body(), responseType);
    }

    private ExternalReviewResponseDto toExternalReview(KinopoiskReviewDto review) {
        return ExternalReviewResponseDto.builder()
                .author(review.author() == null || review.author().isBlank() ? "Пользователь Кинопоиска" : review.author())
                .content(sanitizeReviewText(review.review()))
                .rating(toDouble(review.userRating()))
                .sourceUrl(null)
                .createdAt(parseDateTime(review.date()))
                .build();
    }

    private String sanitizeReviewText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return rawText;
        }

        String withLineBreaks = rawText.replaceAll("(?i)<br\\s*/?>", "\n");
        String withoutTags = withLineBreaks.replaceAll("<[^>]+>", "");
        String unescaped = HtmlUtils.htmlUnescape(withoutTags);

        return unescaped
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private Double toDouble(Number value) {
        return value == null ? null : value.doubleValue();
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private int scoreResult(KinopoiskMovieDto result, String normalizedTitle, Integer releaseYear) {
        int score = 0;
        String normalizedResultName = normalize(result.name());
        String normalizedAltName = normalize(result.alternativeName());
        String normalizedEnName = normalize(result.enName());

        if (!normalizedTitle.isBlank()) {
            if (normalizedTitle.equals(normalizedResultName)
                    || normalizedTitle.equals(normalizedAltName)
                    || normalizedTitle.equals(normalizedEnName)) {
                score += 100;
            } else if (containsEitherWay(normalizedTitle, normalizedResultName)
                    || containsEitherWay(normalizedTitle, normalizedAltName)
                    || containsEitherWay(normalizedTitle, normalizedEnName)) {
                score += 60;
            }
        }

        if (releaseYear != null && result.year() != null) {
            if (releaseYear.equals(result.year())) {
                score += 30;
            } else if (Math.abs(releaseYear - result.year()) == 1) {
                score += 10;
            }
        }

        return score;
    }

    private boolean containsEitherWay(String expected, String actual) {
        if (actual == null || actual.isBlank() || expected == null || expected.isBlank()) {
            return false;
        }
        return actual.contains(expected) || expected.contains(actual);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KinopoiskMovieSearchResponse(List<KinopoiskMovieDto> docs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KinopoiskReviewResponse(List<KinopoiskReviewDto> docs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KinopoiskMovieDto(
            Long id,
            String name,
            String alternativeName,
            String enName,
            Integer year
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KinopoiskReviewDto(
            String author,
            String review,
            String date,
            Number userRating
    ) {
    }
}
