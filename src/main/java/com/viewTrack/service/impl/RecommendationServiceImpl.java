package com.viewTrack.service.impl;

import com.viewTrack.data.entity.*;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.data.repository.UserMovieRepository;
import com.viewTrack.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int DEFAULT_LIMIT = 12;
    private static final float MIN_SCORE = 0.35f;
    private static final Set<String> STOP_WORDS = Set.of("и", "в", "во", "на", "по", "с", "со", "а", "но", "или",
            "как", "для", "это", "этот", "эта", "что", "где", "когда", "над", "под", "из", "без", "у", "о", "об", "от",
            "до", "за", "перед", "через", "между", "около", "возле", "мимо", "кроме", "внутри", "снаружи", "вместо",
            "ради", "благодаря", "согласно", "вопреки", "несмотря", "ввиду", "вследствие", "вслед", "навстречу",
            "сквозь", "из-за", "из-под", "же", "бы", "не", "ни", "да", "нет", "ведь", "даже", "ли", "разве", "неужели",
            "вдруг", "едва", "лишь", "только", "чуть", "почти", "совсем", "абсолютно", "относительно", "очень",
            "сильно", "слабо", "немного", "слегка", "вполне", "совершенно", "достаточно", "слишком", "чрезвычайно",
            "весьма", "исключительно", "особенно", "примерно", "ровно", "как раз", "именно", "прямо", "косвенно",
            "отчасти", "полностью", "всего", "все", "всё", "вся", "всей", "всю", "всех", "всем", "всеми", "всём", "эти",
            "этих", "этим", "этими", "этой", "этом", "этому", "эту", "тот", "та", "те", "тех", "тем", "теми", "такой",
            "такая", "такое", "такие", "такого", "таких", "таким", "такими", "так", "также", "тоже", "то",
            "того", "тому", "зато", "однако", "хотя", "пусть", "пускай", "чтобы", "будто", "словно", "если",
            "раз", "потому", "поэтому", "оттого", "итак", "следовательно", "значит", "например", "во-первых",
            "во-вторых", "наконец", "вообще", "в общем", "в частности", "то есть"
    );

    private final MovieRepository movieRepository;
    private final UserMovieRepository userMovieRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public List<Movie> getRecommendations(User user, int limit) {
        int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        List<Movie> allMovies = movieRepository.findAllWithGenresAndDirectors();
        if (allMovies.isEmpty()) {
            return List.of();
        }

        List<UserMovie> userMovies = userMovieRepository.findByUser(user);
        Set<Long> excludedMovieIds = userMovies.stream()
                .map(userMovie -> userMovie.getMovie().getId())
                .collect(Collectors.toSet());

        List<Review> userReviews = reviewRepository.findByUser(user);
        if (userReviews.isEmpty() && excludedMovieIds.isEmpty()) {
            return allMovies.stream()
                    .sorted(Comparator.comparing(Movie::getAverageRating).reversed())
                    .limit(effectiveLimit)
                    .toList();
        }

        Map<Long, Integer> userRatings = userReviews.stream()
                .filter(review -> review.getMovie() != null && review.getMovie().getId() != null)
                .collect(Collectors.toMap(
                        review -> review.getMovie().getId(),
                        Review::getRating,
                        (left, right) -> right
                ));

        Map<String, Float> preferredGenres = new HashMap<>();
        Map<Long, Float> preferredDirectors = new HashMap<>();
        Set<String> preferredTextTokens = new HashSet<>();

        for (Movie movie : allMovies) {
            if (!excludedMovieIds.contains(movie.getId()) && !userRatings.containsKey(movie.getId())) {
                continue;
            }

            int rating = userRatings.getOrDefault(movie.getId(), 7);
            float weight = toPreferenceWeight(rating);

            for (Genre genre : movie.getGenres()) {
                preferredGenres.merge(genre.getGenreName(), weight, Float::sum);
            }
            for (Director director : movie.getDirectors()) {
                preferredDirectors.merge(director.getId(), weight, Float::sum);
            }
        }

        for (Review review : userReviews) {
            if (review.getContent() == null || review.getContent().isBlank()) {
                continue;
            }

            float textWeight = toPreferenceWeight(review.getRating());
            if (textWeight <= 0) {
                continue;
            }

            preferredTextTokens.addAll(tokenize(review.getContent()));
        }

        if (preferredGenres.isEmpty() && preferredDirectors.isEmpty()) {
            return allMovies.stream()
                    .filter(movie -> !excludedMovieIds.contains(movie.getId()))
                    .sorted(Comparator.comparing(Movie::getAverageRating).reversed())
                    .limit(effectiveLimit)
                    .toList();
        }

        List<MovieScore> scored = new ArrayList<>();
        for (Movie movie : allMovies) {
            if (excludedMovieIds.contains(movie.getId())) {
                continue;
            }

            float score = 0;
            for (Genre genre : movie.getGenres()) {
                score += preferredGenres.getOrDefault(genre.getGenreName(), 0f) * 1.6f;
            }
            for (Director director : movie.getDirectors()) {
                score += preferredDirectors.getOrDefault(director.getId(), 0f) * 1.9f;
            }

            score += Math.max(movie.getAverageRating(), 0) * 0.45f;

            if (!preferredTextTokens.isEmpty()) {
                Set<String> movieTokens = tokenize(movie.getTitle() + " " + safeText(movie.getDescription()));
                if (!movieTokens.isEmpty()) {
                    long matches = movieTokens.stream().filter(preferredTextTokens::contains).count();
                    score += ((float) matches / movieTokens.size()) * 2.5f;
                }
            }

            if (score >= MIN_SCORE) {
                scored.add(new MovieScore(movie, score));
            }
        }

        return scored.stream()
                .sorted(Comparator.comparing(MovieScore::score).reversed())
                .limit(effectiveLimit)
                .map(MovieScore::movie)
                .toList();
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static float toPreferenceWeight(int rating) {
        if (rating <= 0) {
            return 0f;
        }
        return Math.max(0f, (rating - 4) / 6f);
    }

    private static Set<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .lines()
                .flatMap(line -> List.of(line.split(" ")).stream())
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private record MovieScore(Movie movie, float score) {
    }
}
