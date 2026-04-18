package com.viewTrack.service.impl;

import chat.giga.client.GigaChatClient;
import chat.giga.model.ModelName;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viewTrack.data.entity.Director;
import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.service.GigaChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GigaChatServiceImpl implements GigaChatService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final int MAX_MOVIE_DESCRIPTION_LENGTH = 260;

    private final GigaChatClient gigaChatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String generateMovieReview(Movie movie, List<Review> reviews) {
        try {
            String prompt = buildReviewPrompt(movie, reviews);

            ChatMessage message = ChatMessage.builder()
                    .role(ChatMessage.Role.USER)
                    .content(prompt)
                    .build();

            CompletionRequest request = CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT_MAX)
                    .messages(List.of(message))
                    .build();

            CompletionResponse response = gigaChatClient.completions(request);
            
            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content();
            } else {
                return "Не удалось сгенерировать рецензию";
            }

        } catch (Exception e) {
            return "Произошла ошибка при генерации рецензии: " + e.getMessage();
        }
    }

    @Override
    public List<Long> rankMovieRecommendations(String userTasteProfile, List<Movie> candidateMovies, int limit) {
        if (candidateMovies == null || candidateMovies.isEmpty() || limit <= 0) {
            return List.of();
        }

        try {
            String prompt = buildRecommendationPrompt(userTasteProfile, candidateMovies, limit);
            CompletionResponse response = requestCompletion(prompt);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String content = response.choices().get(0).message().content();
                return extractRecommendationIds(content, limit);
            }
        } catch (Exception e) {
            log.error("Ошибка при AI-ранжировании рекомендаций: {}", e.getMessage(), e);
        }

        return List.of();
    }

    private String buildReviewPrompt(Movie movie, List<Review> reviews) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - профессиональный кинокритик. Напиши краткую рецензию на фильм на основе отзывов зрителей.");
        prompt.append("Важно: НЕ раскрывай сюжет и спойлеры! Фокусируйся на общих впечатлениях, атмосфере, актерской игре, режиссуре.");
        prompt.append("Учитывай как положительные, так и отрицательные отзывы. Пиши рецензию только на основе отзывов зрителей, не придумывай ничего сам.\n\n");
        
        prompt.append("Информация о фильме:\n");
        prompt.append("- Название: ").append(movie.getTitle()).append("\n");
        if (movie.getReleaseDate() != null) {
            prompt.append("- Год выпуска: ").append(movie.getReleaseDate().getYear()).append("\n");
        }
        if (movie.getDurationMin() > 0) {
            prompt.append("- Длительность: ").append(movie.getDurationMin()).append(" мин\n");
        }
        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            prompt.append("- Жанры: ").append(movie.getGenres().stream()
                    .map(Genre::getGenreName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Не указан")).append("\n");
        }
        
        prompt.append("\nОтзывы зрителей:\n");
        for (Review review : reviews) {
            prompt.append("- Оценка ").append(review.getRating()).append("/10: ")
                    .append(review.getContent()).append("\n");
        }
        
        prompt.append("\nНапиши рецензию объемом 100 слов, избегая спойлеров. ");
        prompt.append("Используй информацию из отзывов для анализа качества фильма.");
        
        return prompt.toString();
    }

    private String buildRecommendationPrompt(String userTasteProfile, List<Movie> candidateMovies, int limit) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - интеллектуальная рекомендательная система фильмов.\n");
        prompt.append("Тебе дан профиль вкуса пользователя и список кандидатов.\n");
        prompt.append("Выбери фильмы, которые вероятнее всего понравятся пользователю.\n\n");

        prompt.append("Строгие правила ответа:\n");
        prompt.append("1. Используй только фильмы из списка кандидатов.\n");
        prompt.append("2. Не придумывай новые id.\n");
        prompt.append("3. Учитывай любимые и нелюбимые жанры, режиссеров, атмосферу и темы из отзывов.\n");
        prompt.append("4. Верни только JSON без markdown и пояснений.\n");
        prompt.append("5. Формат ответа строго такой: {\"recommendedIds\":[1,2,3]}.\n");
        prompt.append("6. Верни не более ").append(limit).append(" id в порядке от лучшего совпадения к худшему.\n\n");

        prompt.append("Профиль пользователя:\n");
        prompt.append(userTasteProfile);
        prompt.append("\n\nСписок кандидатов:\n");

        for (Movie movie : candidateMovies) {
            prompt.append("- id=").append(movie.getId())
                    .append(" | название=").append(safeText(movie.getTitle()))
                    .append(" | год=").append(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : "не указан")
                    .append(" | жанры=").append(joinGenres(movie))
                    .append(" | режиссеры=").append(joinDirectors(movie))
                    .append(" | средний_рейтинг=").append(movie.getAverageRating() > 0 ? String.format("%.1f", movie.getAverageRating()) : "нет")
                    .append(" | описание=").append(truncate(safeText(movie.getDescription()), MAX_MOVIE_DESCRIPTION_LENGTH))
                    .append("\n");
        }

        return prompt.toString();
    }

    private CompletionResponse requestCompletion(String prompt) {
        ChatMessage message = ChatMessage.builder()
                .role(ChatMessage.Role.USER)
                .content(prompt)
                .build();

        CompletionRequest request = CompletionRequest.builder()
                .model(ModelName.GIGA_CHAT_MAX)
                .messages(List.of(message))
                .build();

        return gigaChatClient.completions(request);
    }

    private List<Long> extractRecommendationIds(String responseContent, int limit) {
        if (responseContent == null || responseContent.isBlank()) {
            return List.of();
        }

        String normalizedContent = responseContent
                .replace("```json", "")
                .replace("```", "")
                .trim();

        List<Long> jsonIds = extractIdsFromJson(normalizedContent, limit);
        if (!jsonIds.isEmpty()) {
            return jsonIds;
        }

        LinkedHashSet<Long> fallbackIds = new LinkedHashSet<>();
        Matcher matcher = NUMBER_PATTERN.matcher(normalizedContent);
        while (matcher.find() && fallbackIds.size() < limit) {
            fallbackIds.add(Long.parseLong(matcher.group()));
        }

        return List.copyOf(fallbackIds);
    }

    private List<Long> extractIdsFromJson(String responseContent, int limit) {
        try {
            JsonNode root = objectMapper.readTree(responseContent);
            JsonNode idsNode = root.get("recommendedIds");
            if (idsNode == null || !idsNode.isArray()) {
                return List.of();
            }

            LinkedHashSet<Long> ids = new LinkedHashSet<>();
            for (JsonNode idNode : idsNode) {
                if (idNode.canConvertToLong()) {
                    ids.add(idNode.longValue());
                } else if (idNode.isTextual()) {
                    try {
                        ids.add(Long.parseLong(idNode.asText().trim()));
                    } catch (NumberFormatException ignored) {
                        log.debug("Не удалось распарсить текстовый id рекомендации: {}", idNode.asText());
                    }
                }

                if (ids.size() >= limit) {
                    break;
                }
            }

            return List.copyOf(ids);
        } catch (Exception e) {
            log.warn("Не удалось распарсить AI-рекомендации как JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String joinGenres(Movie movie) {
        Set<Genre> genres = movie.getGenres();
        if (genres == null || genres.isEmpty()) {
            return "не указаны";
        }

        return genres.stream()
                .map(Genre::getGenreName)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("не указаны");
    }

    private String joinDirectors(Movie movie) {
        Set<Director> directors = movie.getDirectors();
        if (directors == null || directors.isEmpty()) {
            return "не указаны";
        }

        return directors.stream()
                .map(Director::getFullName)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("не указаны");
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }

    private static String safeText(String value) {
        return value == null || value.isBlank() ? "не указано" : value.trim();
    }
}
