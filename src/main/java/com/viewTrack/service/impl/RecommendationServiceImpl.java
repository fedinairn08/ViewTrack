package com.viewTrack.service.impl;

import com.viewTrack.data.entity.*;
import com.viewTrack.data.enums.Type;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.data.repository.UserMovieRepository;
import com.viewTrack.dto.response.AiRecommendationRankingResult;
import com.viewTrack.dto.response.RecommendationsResult;
import com.viewTrack.service.GigaChatService;
import com.viewTrack.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int DEFAULT_LIMIT = 12;
    private static final int MIN_AI_CANDIDATE_POOL = 18;
    private static final int MAX_AI_CANDIDATE_POOL = 30;
    private static final int AI_CANDIDATE_MULTIPLIER = 3;
    private static final int MAX_PROFILE_POSITIVE_ITEMS = 6;
    private static final int MAX_PROFILE_NEGATIVE_ITEMS = 4;
    private static final int MAX_PROFILE_TO_WATCH_ITEMS = 6;
    private static final int MIN_TEXT_REVIEW_RATING = 7;
    private static final int MAX_REVIEW_EXCERPT_LENGTH = 180;
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
    private final GigaChatService gigaChatService;

    @Override
    public RecommendationsResult getRecommendations(User user, int limit) {
        int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        int candidatePoolSize = resolveCandidatePoolSize(effectiveLimit);
        List<Movie> allMovies = movieRepository.findAllWithGenresAndDirectors();
        if (allMovies.isEmpty()) {
            return RecommendationsResult.ofMoviesOnly(List.of());
        }

        List<UserMovie> userMovies = userMovieRepository.findByUser(user);
        Set<Long> excludedMovieIds = new HashSet<>(extractExcludedMovieIds(userMovies));

        List<Review> userReviews = reviewRepository.findByUser(user);
        if (userReviews.isEmpty() && excludedMovieIds.isEmpty()) {
            return RecommendationsResult.ofMoviesOnly(topRatedMovies(allMovies, excludedMovieIds, Set.of(), effectiveLimit));
        }

        Map<Long, Integer> userRatings = extractUserRatings(userReviews);
        excludedMovieIds.addAll(userRatings.keySet());

        Set<Long> preferenceMovieIds = new HashSet<>(excludedMovieIds);

        Set<Long> avoidedDirectorIds = computeAvoidedDirectorIds(allMovies, userRatings);
        PreferenceSnapshot preferenceSnapshot = buildPreferenceSnapshot(allMovies, preferenceMovieIds, userReviews, userRatings, avoidedDirectorIds);
        List<Movie> heuristicRankedMovies = rankMoviesHeuristically(allMovies, excludedMovieIds, preferenceSnapshot, candidatePoolSize);
        List<Movie> fallbackRecommendations = heuristicRankedMovies.stream()
                .limit(effectiveLimit)
                .toList();

        if (heuristicRankedMovies.isEmpty()) {
            return RecommendationsResult.ofMoviesOnly(topRatedMovies(allMovies, excludedMovieIds, avoidedDirectorIds, effectiveLimit));
        }

        List<Movie> candidateMovies = buildCandidatePool(allMovies, excludedMovieIds, preferenceSnapshot, heuristicRankedMovies, candidatePoolSize);
        if (candidateMovies.isEmpty()) {
            return RecommendationsResult.ofMoviesOnly(fallbackRecommendations);
        }

        String userTasteProfile = buildUserTasteProfile(allMovies, userMovies, userReviews, preferenceSnapshot);
        AiRecommendationRankingResult aiRanking = gigaChatService.rankMovieRecommendations(userTasteProfile, candidateMovies, effectiveLimit);
        List<Movie> aiRankedMovies = mapAiIdsToMovies(aiRanking.recommendedIds(), candidateMovies);
        aiRankedMovies = filterOutAvoidedDirectors(aiRankedMovies, preferenceSnapshot.avoidedDirectorIds());

        if (aiRankedMovies.isEmpty()) {
            log.info("AI не вернул валидный список рекомендаций, используется эвристический fallback");
            return RecommendationsResult.ofMoviesOnly(fallbackRecommendations);
        }

        MergedRecommendations merged = mergeRecommendations(
                aiRankedMovies,
                aiRanking.explanationByMovieId(),
                fallbackRecommendations,
                effectiveLimit
        );
        return new RecommendationsResult(merged.movies(), merged.explanationByMovieId());
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static String safeTitle(Movie movie) {
        return safeText(movie.getTitle());
    }

    private static Set<Genre> safeGenres(Movie movie) {
        return movie.getGenres() == null ? Set.of() : movie.getGenres();
    }

    private static Set<Director> safeDirectors(Movie movie) {
        return movie.getDirectors() == null ? Set.of() : movie.getDirectors();
    }

    private static float safeAverageRating(Movie movie) {
        return Math.max(movie.getAverageRating(), 0);
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

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }

    private int resolveCandidatePoolSize(int effectiveLimit) {
        int multipliedLimit = effectiveLimit * AI_CANDIDATE_MULTIPLIER;
        return Math.max(MIN_AI_CANDIDATE_POOL, Math.min(MAX_AI_CANDIDATE_POOL, multipliedLimit));
    }

    private Set<Long> extractExcludedMovieIds(List<UserMovie> userMovies) {
        return userMovies.stream()
                .map(UserMovie::getMovie)
                .filter(Objects::nonNull)
                .map(Movie::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, Integer> extractUserRatings(List<Review> userReviews) {
        return userReviews.stream()
                .filter(review -> review.getMovie() != null && review.getMovie().getId() != null)
                .collect(Collectors.toMap(
                        review -> review.getMovie().getId(),
                        Review::getRating,
                        (left, right) -> right
                ));
    }

    /**
     * Режиссёр в «чёрном списке», если по его фильмам у пользователя была оценка не выше 3,
     * при этом ни один его фильм не получил у пользователя 8+ (иначе вкус смешанный — не отсекаем).
     */
    private Set<Long> computeAvoidedDirectorIds(List<Movie> allMovies, Map<Long, Integer> userRatings) {
        if (userRatings == null || userRatings.isEmpty()) {
            return Set.of();
        }
        Map<Long, Movie> byId = allMovies.stream()
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(Movie::getId, m -> m, (a, b) -> a));
        Map<Long, int[]> minMaxByDirector = new HashMap<>();
        for (Map.Entry<Long, Integer> e : userRatings.entrySet()) {
            Movie movie = byId.get(e.getKey());
            if (movie == null) {
                continue;
            }
            int rating = e.getValue();
            for (Director director : safeDirectors(movie)) {
                if (director.getId() == null) {
                    continue;
                }
                minMaxByDirector.merge(director.getId(), new int[]{rating, rating},
                        (a, b) -> new int[]{Math.min(a[0], b[0]), Math.max(a[1], b[1])});
            }
        }
        Set<Long> avoided = new HashSet<>();
        for (Map.Entry<Long, int[]> e : minMaxByDirector.entrySet()) {
            int min = e.getValue()[0];
            int max = e.getValue()[1];
            if (min <= 3 && max < 8) {
                avoided.add(e.getKey());
            }
        }
        return Set.copyOf(avoided);
    }

    private static boolean movieHasAvoidedDirector(Movie movie, Set<Long> avoidedDirectorIds) {
        if (avoidedDirectorIds == null || avoidedDirectorIds.isEmpty()) {
            return false;
        }
        for (Director director : safeDirectors(movie)) {
            if (director.getId() != null && avoidedDirectorIds.contains(director.getId())) {
                return true;
            }
        }
        return false;
    }

    private static List<Movie> filterOutAvoidedDirectors(List<Movie> movies, Set<Long> avoidedDirectorIds) {
        if (movies == null || movies.isEmpty() || avoidedDirectorIds == null || avoidedDirectorIds.isEmpty()) {
            return movies;
        }
        return movies.stream()
                .filter(m -> !movieHasAvoidedDirector(m, avoidedDirectorIds))
                .toList();
    }

    private static String ensureSentenceCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String t = raw.strip();
        int idx = 0;
        while (idx < t.length()) {
            int cp = t.codePointAt(idx);
            if (Character.isLetter(cp)) {
                int upper = Character.toUpperCase(cp);
                if (upper != cp) {
                    return t.substring(0, idx) + new String(Character.toChars(upper)) + t.substring(idx + Character.charCount(cp));
                }
                return t;
            }
            idx += Character.charCount(cp);
        }
        return t;
    }

    private static String polishRecommendationExplanation(String raw, Movie movie) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String s = ensureSentenceCase(raw.strip());
        s = deFluffUserPhrasing(s);
        s = stripSelfComparisonToCandidateTitle(s, movie != null ? safeTitle(movie) : "");
        s = collapseWhitespace(s);
        return ensureTrailingPeriod(s);
    }

    private static String collapseWhitespace(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("\\s{2,}", " ").strip();
    }

    private static String ensureTrailingPeriod(String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        String t = s.strip();
        char last = t.charAt(t.length() - 1);
        if (last == '.' || last == '!' || last == '?' || last == '…') {
            return t;
        }
        return t + '.';
    }

    private static String deFluffUserPhrasing(String s) {
        if (s == null) {
            return null;
        }
        String t = s;
        String[][] repl = {
                {"(?iu)\\bдля пользователя\\b", "вам"},
                {"(?iu)\\bу пользователя\\b", "у вас"},
                {"(?iu)\\bк пользователю\\b", "вам"},
                {"(?iu)\\bинтересы пользователя\\b", "ваш вкус"},
                {"(?iu)\\bинтересам пользователя\\b", "вашим вкусам"},
                {"(?iu)\\bэтот пользователь\\b", "вы"},
                {"(?iu)\\bданный пользователь\\b", "вы"},
                {"(?iu)\\bсам пользователь\\b", "вы"},
                {"(?iu)\\bпользователь считает\\b", "вы считаете"},
                {"(?iu)\\bпользователь оценил\\b", "вы оценили"},
                {"(?iu)\\bпользователь любит\\b", "вы любите"},
                {"(?iu)\\b,\\s*пользователь\\b", ", вы"},
        };
        for (String[] p : repl) {
            t = t.replaceAll(p[0], p[1]);
        }
        t = t.replaceAll("(?iu)\\s+пользователь(\\s)", " вы$1");
        t = t.replaceAll("(?iu)^пользователь(\\s)", "Вы$1");
        return t;
    }

    /**
     * Убирает фразы вроде «любимый фильм "Назад в будущее"», когда рекомендуется тот же фильм.
     */
    private static String stripSelfComparisonToCandidateTitle(String reason, String movieTitle) {
        if (reason == null || movieTitle == null || movieTitle.isBlank()) {
            return reason;
        }
        String title = movieTitle.strip();
        if (title.length() < 2) {
            return reason;
        }
        String low = reason.toLowerCase(Locale.ROOT);
        String titleLow = title.toLowerCase(Locale.ROOT);
        if (!low.contains(titleLow)) {
            return reason;
        }
        String quoted = Pattern.quote(title);
        Pattern selfRef = Pattern.compile(
                "(?iu)[^.?!]*(?:любим(ым|ого|ому|ый)\\s+фильм(ом|а)?|схож(ие|их)?\\s+предпочтения|в\\s+том\\s+же\\s+ключе|в\\s+духе)[^.?!]*[«\"']?"
                        + quoted + "[»\"']?[^.?!]*[.?!]?");
        Matcher m = selfRef.matcher(reason);
        if (!m.find()) {
            return reason;
        }
        String cleaned = m.replaceFirst(" ").strip();
        cleaned = cleaned.replaceAll("^[;,\\s–\\-:]+", "").replaceAll("\\s{2,}", " ");
        return cleaned.length() < 12 ? reason : cleaned;
    }

    private PreferenceSnapshot buildPreferenceSnapshot(List<Movie> allMovies,
                                                      Set<Long> preferenceMovieIds,
                                                      List<Review> userReviews,
                                                      Map<Long, Integer> userRatings,
                                                      Set<Long> avoidedDirectorIds) {
        Map<String, Float> preferredGenres = new HashMap<>();
        Map<Long, Float> preferredDirectors = new HashMap<>();
        Set<String> preferredTextTokens = new HashSet<>();

        for (Movie movie : allMovies) {
            if (movie.getId() == null || !preferenceMovieIds.contains(movie.getId())) {
                continue;
            }

            int rating = userRatings.getOrDefault(movie.getId(), 7);
            float weight = toPreferenceWeight(rating);
            if (weight <= 0) {
                continue;
            }

            for (Genre genre : safeGenres(movie)) {
                preferredGenres.merge(genre.getGenreName(), weight, Float::sum);
            }
            for (Director director : safeDirectors(movie)) {
                if (director.getId() != null) {
                    preferredDirectors.merge(director.getId(), weight, Float::sum);
                }
            }
        }

        for (Review review : userReviews) {
            if (review.getContent() == null || review.getContent().isBlank()) {
                continue;
            }

            if (review.getRating() < MIN_TEXT_REVIEW_RATING) {
                continue;
            }

            preferredTextTokens.addAll(tokenize(review.getContent()));
        }

        Set<Long> avoided = avoidedDirectorIds == null ? Set.of() : avoidedDirectorIds;
        return new PreferenceSnapshot(userRatings, preferredGenres, preferredDirectors, preferredTextTokens, avoided);
    }

    private List<Movie> rankMoviesHeuristically(List<Movie> allMovies,
                                                Set<Long> excludedMovieIds,
                                                PreferenceSnapshot preferenceSnapshot,
                                                int limit) {
        if (preferenceSnapshot.isEmpty()) {
            return topRatedMovies(allMovies, excludedMovieIds, preferenceSnapshot.avoidedDirectorIds(), limit);
        }

        List<MovieScore> scored = new ArrayList<>();
        for (Movie movie : allMovies) {
            if (movie.getId() == null || excludedMovieIds.contains(movie.getId())) {
                continue;
            }
            if (movieHasAvoidedDirector(movie, preferenceSnapshot.avoidedDirectorIds())) {
                continue;
            }

            float score = 0;
            for (Genre genre : safeGenres(movie)) {
                score += preferenceSnapshot.preferredGenres().getOrDefault(genre.getGenreName(), 0f) * 1.6f;
            }
            for (Director director : safeDirectors(movie)) {
                if (director.getId() != null) {
                    score += preferenceSnapshot.preferredDirectors().getOrDefault(director.getId(), 0f) * 1.9f;
                }
            }

            score += safeAverageRating(movie) * 0.45f;

            if (!preferenceSnapshot.preferredTextTokens().isEmpty()) {
                Set<String> movieTokens = tokenize(movie.getTitle() + " " + safeText(movie.getDescription()));
                if (!movieTokens.isEmpty()) {
                    long matches = movieTokens.stream()
                            .filter(preferenceSnapshot.preferredTextTokens()::contains)
                            .count();
                    score += ((float) matches / movieTokens.size()) * 2.5f;
                }
            }

            if (score >= MIN_SCORE) {
                scored.add(new MovieScore(movie, score));
            }
        }

        List<Movie> ranked = scored.stream()
                .sorted(Comparator.comparing(MovieScore::score)
                        .reversed()
                        .thenComparing(movieScore -> safeTitle(movieScore.movie())))
                .limit(limit)
                .map(MovieScore::movie)
                .toList();

        if (!ranked.isEmpty()) {
            return ranked;
        }

        return topRatedMovies(allMovies, excludedMovieIds, preferenceSnapshot.avoidedDirectorIds(), limit);
    }

    private List<Movie> buildCandidatePool(List<Movie> allMovies,
                                           Set<Long> excludedMovieIds,
                                           PreferenceSnapshot preferenceSnapshot,
                                           List<Movie> heuristicRankedMovies,
                                           int candidatePoolSize) {
        LinkedHashMap<Long, Movie> candidateMovies = new LinkedHashMap<>();

        heuristicRankedMovies.stream()
                .limit(candidatePoolSize)
                .forEach(movie -> candidateMovies.putIfAbsent(movie.getId(), movie));

        if (candidateMovies.size() < candidatePoolSize) {
            topRatedMovies(allMovies, excludedMovieIds, preferenceSnapshot.avoidedDirectorIds(), candidatePoolSize).forEach(movie ->
                    candidateMovies.putIfAbsent(movie.getId(), movie));
        }

        return candidateMovies.values().stream()
                .limit(candidatePoolSize)
                .toList();
    }

    private List<Movie> topRatedMovies(List<Movie> allMovies,
                                       Set<Long> excludedMovieIds,
                                       Set<Long> avoidedDirectorIds,
                                       int limit) {
        Set<Long> avoided = avoidedDirectorIds == null ? Set.of() : avoidedDirectorIds;
        return allMovies.stream()
                .filter(movie -> movie.getId() == null || !excludedMovieIds.contains(movie.getId()))
                .filter(movie -> !movieHasAvoidedDirector(movie, avoided))
                .sorted(Comparator.comparing(RecommendationServiceImpl::safeAverageRating)
                        .reversed()
                        .thenComparing(RecommendationServiceImpl::safeTitle))
                .limit(limit)
                .toList();
    }

    private String buildUserTasteProfile(List<Movie> allMovies,
                                         List<UserMovie> userMovies,
                                         List<Review> userReviews,
                                         PreferenceSnapshot preferenceSnapshot) {
        Map<Long, Movie> moviesById = allMovies.stream()
                .filter(movie -> movie.getId() != null)
                .collect(Collectors.toMap(Movie::getId, movie -> movie, (left, right) -> left));

        Map<Long, String> directorsById = allMovies.stream()
                .flatMap(movie -> safeDirectors(movie).stream())
                .filter(director -> director.getId() != null)
                .collect(Collectors.toMap(
                        Director::getId,
                        Director::getFullName,
                        (left, right) -> left
                ));

        StringBuilder profile = new StringBuilder("Профиль предпочтений пользователя:\n");
        appendPositiveReviews(profile, userReviews, moviesById);
        appendNegativeReviews(profile, userReviews, moviesById);
        appendToWatchList(profile, userMovies, moviesById);
        appendAvoidedDirectors(profile, preferenceSnapshot, directorsById);
        appendPreferenceSummary(profile, preferenceSnapshot, directorsById);

        if (profile.toString().trim().equals("Профиль предпочтений пользователя:")) {
            return "Данных о предпочтениях мало. Сильнее опирайся на средний рейтинг, жанровое сходство и разнообразие рекомендаций.";
        }

        return profile.toString().trim();
    }

    private void appendPositiveReviews(StringBuilder profile, List<Review> userReviews, Map<Long, Movie> moviesById) {
        List<Review> positiveReviews = userReviews.stream()
                .filter(review -> review.getMovie() != null && review.getMovie().getId() != null)
                .filter(review -> review.getRating() >= 8)
                .sorted(Comparator.comparingInt(Review::getRating).reversed())
                .limit(MAX_PROFILE_POSITIVE_ITEMS)
                .toList();

        if (positiveReviews.isEmpty()) {
            return;
        }

        profile.append("Высоко оцененные фильмы:\n");
        for (Review review : positiveReviews) {
            Movie movie = moviesById.getOrDefault(review.getMovie().getId(), review.getMovie());
            profile.append("- ").append(describeMovie(movie))
                    .append(" | оценка=").append(review.getRating()).append("/10");

            if (review.getContent() != null && !review.getContent().isBlank()) {
                profile.append(" | отзыв=").append(truncate(review.getContent().trim(), MAX_REVIEW_EXCERPT_LENGTH));
            }
            profile.append("\n");
        }
        profile.append("\n");
    }

    private void appendNegativeReviews(StringBuilder profile, List<Review> userReviews, Map<Long, Movie> moviesById) {
        List<Review> negativeReviews = userReviews.stream()
                .filter(review -> review.getMovie() != null && review.getMovie().getId() != null)
                .filter(review -> review.getRating() <= 5)
                .sorted(Comparator.comparingInt(Review::getRating))
                .limit(MAX_PROFILE_NEGATIVE_ITEMS)
                .toList();

        if (negativeReviews.isEmpty()) {
            return;
        }

        profile.append("Низко оцененные фильмы:\n");
        for (Review review : negativeReviews) {
            Movie movie = moviesById.getOrDefault(review.getMovie().getId(), review.getMovie());
            profile.append("- ").append(describeMovie(movie))
                    .append(" | оценка=").append(review.getRating()).append("/10");

            if (review.getContent() != null && !review.getContent().isBlank()) {
                profile.append(" | отзыв=").append(truncate(review.getContent().trim(), MAX_REVIEW_EXCERPT_LENGTH));
            }
            profile.append("\n");
        }
        profile.append("\n");
    }

    private void appendToWatchList(StringBuilder profile, List<UserMovie> userMovies, Map<Long, Movie> moviesById) {
        List<Movie> plannedMovies = userMovies.stream()
                .filter(userMovie -> Type.TO_WATCH.equals(userMovie.getType()))
                .map(UserMovie::getMovie)
                .filter(Objects::nonNull)
                .map(movie -> movie.getId() == null ? movie : moviesById.getOrDefault(movie.getId(), movie))
                .limit(MAX_PROFILE_TO_WATCH_ITEMS)
                .toList();

        if (plannedMovies.isEmpty()) {
            return;
        }

        profile.append("Фильмы в списке \"Буду смотреть\":\n");
        for (Movie movie : plannedMovies) {
            profile.append("- ").append(describeMovie(movie)).append("\n");
        }
        profile.append("\n");
    }

    private void appendAvoidedDirectors(StringBuilder profile,
                                        PreferenceSnapshot preferenceSnapshot,
                                        Map<Long, String> directorsById) {
        if (preferenceSnapshot.avoidedDirectorIds().isEmpty()) {
            return;
        }
        profile.append("Не предлагать фильмы этих режиссёров (у пользователя к их работам низкие оценки без контрастных высоких):\n");
        for (Long directorId : preferenceSnapshot.avoidedDirectorIds()) {
            String name = directorsById.get(directorId);
            if (name != null && !name.isBlank()) {
                profile.append("- ").append(name).append("\n");
            }
        }
        profile.append("\n");
    }

    private void appendPreferenceSummary(StringBuilder profile,
                                         PreferenceSnapshot preferenceSnapshot,
                                         Map<Long, String> directorsById) {
        List<String> topGenres = preferenceSnapshot.preferredGenres().entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(4)
                .map(Map.Entry::getKey)
                .toList();

        List<String> topDirectors = preferenceSnapshot.preferredDirectors().entrySet().stream()
                .sorted(Map.Entry.<Long, Float>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .filter(id -> !preferenceSnapshot.avoidedDirectorIds().contains(id))
                .map(directorsById::get)
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        List<String> topKeywords = preferenceSnapshot.preferredTextTokens().stream()
                .sorted()
                .limit(10)
                .toList();

        if (topGenres.isEmpty() && topDirectors.isEmpty() && topKeywords.isEmpty()) {
            return;
        }

        profile.append("Сводка предпочтений:\n");
        if (!topGenres.isEmpty()) {
            profile.append("- вероятно любимые жанры: ").append(String.join(", ", topGenres)).append("\n");
        }
        if (!topDirectors.isEmpty()) {
            profile.append("- вероятно интересные режиссеры: ").append(String.join(", ", topDirectors)).append("\n");
        }
        if (!topKeywords.isEmpty()) {
            profile.append("- ключевые темы из отзывов: ").append(String.join(", ", topKeywords)).append("\n");
        }
        profile.append("\n");
    }

    private String describeMovie(Movie movie) {
        String year = movie.getReleaseDate() != null ? String.valueOf(movie.getReleaseDate().getYear()) : "год не указан";
        String genres = safeGenres(movie).stream()
                .map(Genre::getGenreName)
                .sorted()
                .collect(Collectors.joining(", "));
        String directors = safeDirectors(movie).stream()
                .map(Director::getFullName)
                .sorted()
                .collect(Collectors.joining(", "));

        return safeText(movie.getTitle())
                + " (" + year + ")"
                + (genres.isBlank() ? "" : " | жанры: " + genres)
                + (directors.isBlank() ? "" : " | режиссеры: " + directors);
    }

    private List<Movie> mapAiIdsToMovies(List<Long> aiRankedIds, List<Movie> candidateMovies) {
        if (aiRankedIds == null || aiRankedIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Movie> candidateMoviesById = candidateMovies.stream()
                .filter(movie -> movie.getId() != null)
                .collect(Collectors.toMap(
                        Movie::getId,
                        movie -> movie,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        LinkedHashMap<Long, Movie> orderedMovies = new LinkedHashMap<>();
        for (Long movieId : aiRankedIds) {
            Movie movie = candidateMoviesById.get(movieId);
            if (movie != null) {
                orderedMovies.putIfAbsent(movieId, movie);
            }
        }

        return List.copyOf(orderedMovies.values());
    }

    private MergedRecommendations mergeRecommendations(List<Movie> aiRankedMovies,
                                                       Map<Long, String> aiExplanationByMovieId,
                                                       List<Movie> fallbackRecommendations,
                                                       int limit) {
        LinkedHashMap<Long, Movie> mergedMovies = new LinkedHashMap<>();
        LinkedHashMap<Long, String> mergedExplanations = new LinkedHashMap<>();

        for (Movie movie : aiRankedMovies) {
            mergedMovies.putIfAbsent(movie.getId(), movie);
            if (aiExplanationByMovieId != null && movie.getId() != null) {
                String text = aiExplanationByMovieId.get(movie.getId());
                if (text != null && !text.isBlank()) {
                    mergedExplanations.putIfAbsent(movie.getId(), polishRecommendationExplanation(text, movie));
                }
            }
        }
        fallbackRecommendations.forEach(movie -> mergedMovies.putIfAbsent(movie.getId(), movie));

        List<Movie> limitedMovies = mergedMovies.values().stream()
                .limit(limit)
                .toList();

        LinkedHashMap<Long, String> explanationsForResult = new LinkedHashMap<>();
        for (Movie movie : limitedMovies) {
            if (movie.getId() != null) {
                String explanation = mergedExplanations.get(movie.getId());
                if (explanation != null) {
                    explanationsForResult.put(movie.getId(), explanation);
                }
            }
        }

        return new MergedRecommendations(limitedMovies, explanationsForResult.isEmpty() ? Map.of() : Map.copyOf(explanationsForResult));
    }

    private record MergedRecommendations(List<Movie> movies, Map<Long, String> explanationByMovieId) {
    }

    private record PreferenceSnapshot(
            Map<Long, Integer> userRatings,
            Map<String, Float> preferredGenres,
            Map<Long, Float> preferredDirectors,
            Set<String> preferredTextTokens,
            Set<Long> avoidedDirectorIds
    ) {
        private PreferenceSnapshot {
            avoidedDirectorIds = avoidedDirectorIds == null ? Set.of() : Set.copyOf(avoidedDirectorIds);
        }

        private boolean isEmpty() {
            return preferredGenres.isEmpty() && preferredDirectors.isEmpty() && preferredTextTokens.isEmpty();
        }
    }

    private record MovieScore(Movie movie, float score) {
    }
}
