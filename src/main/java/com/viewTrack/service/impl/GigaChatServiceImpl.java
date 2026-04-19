package com.viewTrack.service.impl;

import chat.giga.client.GigaChatClient;
import chat.giga.model.ModelName;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.viewTrack.data.entity.Director;
import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.dto.response.AiRecommendationRankingResult;
import com.viewTrack.dto.response.ExternalReviewResponseDto;
import com.viewTrack.service.GigaChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GigaChatServiceImpl implements GigaChatService {

    private static final ObjectMapper LENIENT_AI_JSON = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    private static final Pattern RECOMMENDED_IDS_BLOCK = Pattern.compile(
            "\"recommendedIds\"\\s*:\\s*\\[([^\\]]*)\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern EXPL_MOVIE_REASON = Pattern.compile(
            "\"movieId\"\\s*:\\s*(\\d+)\\s*,\\s*\"reason\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final int MAX_MOVIE_DESCRIPTION_LENGTH = 260;
    private static final int MAX_RECOMMENDATION_REASON_LENGTH = 420;

    private final GigaChatClient gigaChatClient;

    @Override
    public String generateMovieReview(Movie movie,
                                      List<Review> siteReviews,
                                      List<ExternalReviewResponseDto> kinopoiskReviews) {
        try {
            String prompt = buildReviewPrompt(movie, siteReviews, kinopoiskReviews);

            ChatMessage message = ChatMessage.builder()
                    .role(ChatMessage.Role.USER)
                    .content(prompt)
                    .build();

            CompletionRequest request = CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT)
                    .messages(List.of(message))
                    .build();

            CompletionResponse response = gigaChatClient.completions(request);
            
            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return stripMarkdownFormatting(response.choices().get(0).message().content());
            } else {
                return "Не удалось сгенерировать рецензию";
            }

        } catch (Exception e) {
            return "Произошла ошибка при генерации рецензии: " + e.getMessage();
        }
    }

    @Override
    public AiRecommendationRankingResult rankMovieRecommendations(String userTasteProfile, List<Movie> candidateMovies, int limit) {
        if (candidateMovies == null || candidateMovies.isEmpty() || limit <= 0) {
            return AiRecommendationRankingResult.empty();
        }

        try {
            String prompt = buildRecommendationPrompt(userTasteProfile, candidateMovies, limit);
            CompletionResponse response = requestCompletion(prompt);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String content = response.choices().get(0).message().content();
                return parseRecommendationResponse(content, limit);
            }
        } catch (Exception e) {
            log.error("Ошибка при AI-ранжировании рекомендаций: {}", e.getMessage(), e);
        }

        return AiRecommendationRankingResult.empty();
    }

    private String buildReviewPrompt(Movie movie,
                                     List<Review> siteReviews,
                                     List<ExternalReviewResponseDto> kinopoiskReviews) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - профессиональный кинокритик. Напиши краткую рецензию на фильм на основе приведённых отзывов зрителей.");
        prompt.append("Важно: НЕ раскрывай сюжет и спойлеры! Фокусируйся на общих впечатлениях, атмосфере, актерской игре, режиссуре.");
        prompt.append("Учитывай как положительные, так и отрицательные отзывы. Пиши рецензию только на основе этих отзывов, не придумывай ничего сам.\n\n");

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

        if (siteReviews != null && !siteReviews.isEmpty()) {
            prompt.append("\nОтзывы пользователей платформы:\n");
            for (Review review : siteReviews) {
                prompt.append("- Оценка ").append(review.getRating()).append("/10: ")
                        .append(review.getContent()).append("\n");
            }
        }

        if (kinopoiskReviews != null && !kinopoiskReviews.isEmpty()) {
            prompt.append("\nОтзывы зрителей с Кинопоиска:\n");
            for (ExternalReviewResponseDto review : kinopoiskReviews) {
                prompt.append("- ").append(review.getAuthor());
                if (review.getRating() != null && review.getRating() > 0) {
                    prompt.append(", оценка ").append(String.format("%.1f", review.getRating())).append("/10");
                }
                prompt.append(": ").append(review.getContent()).append("\n");
            }
        }

        prompt.append("\nНапиши рецензию объемом 100 слов, избегая спойлеров. ");
        prompt.append("Используй информацию из отзывов для анализа качества фильма. ");
        prompt.append("Пиши обычным связным текстом: без Markdown и HTML — не используй **, *, _, # для выделения и заголовков.");

        return prompt.toString();
    }

    /**
     * Модели часто возвращают Markdown; на странице текст показывается как есть — убираем типичное оформление.
     */
    private static String stripMarkdownFormatting(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String s = raw.strip();
        s = s.replaceAll("\\*\\*([^*]+?)\\*\\*", "$1");
        s = s.replaceAll("__([^_]+?)__", "$1");
        s = s.replace("**", "").replace("__", "");
        return s.strip();
    }

    private String buildRecommendationPrompt(String userTasteProfile, List<Movie> candidateMovies, int limit) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - интеллектуальная рекомендательная система фильмов.\n");
        prompt.append("Тебе дан профиль вкуса пользователя и список кандидатов.\n");
        prompt.append("Выбери фильмы, которые вероятнее всего понравятся этому зрителю (в пояснениях обращайся на «вы»).\n\n");

        prompt.append("Строгие правила ответа:\n");
        prompt.append("1. Используй только фильмы из списка кандидатов.\n");
        prompt.append("2. Не придумывай новые id.\n");
        prompt.append("3. Учитывай любимые и нелюбимые жанры, режиссеров, атмосферу и темы из отзывов.\n");
        prompt.append("4. Верни только JSON без markdown и пояснений.\n");
        prompt.append("5. Формат ответа строго такой: {\"recommendedIds\":[1,2,3],\"explanations\":[{\"movieId\":1,\"reason\":\"одно короткое предложение на русском\"}]}.\n");
        prompt.append("6. Для каждого id из recommendedIds добавь ровно один объект в explanations с тем же movieId и полем reason.\n");
        prompt.append("7. В reason используй только факты из профиля ниже и из строки кандидата (id, жанры, режиссёры, описание); не выдумывай сюжет, порядок частей франшизы и т.п.\n");
        prompt.append("8. Верни не более ").append(limit).append(" id в recommendedIds в порядке от лучшего совпадения к худшему.\n");
        prompt.append("9. Массив recommendedIds закрывай только символом ]; внутри — только целые id через запятую, без скобок.\n");
        prompt.append("10. В поле reason не используй кавычки-клавишу и обратный слэш; пиши по-русски без английских кавычек внутри текста.\n");
        prompt.append("11. Поле reason — ровно одно предложение без переноса строки, в конце обязательна точка.\n");
        prompt.append("12. Каждое reason начинается с заглавной буквы.\n");
        prompt.append("13. Не пиши «первый/второй/третий фильм» о серии: в данных нет номера части.\n");
        prompt.append("14. Обращайся напрямую к зрителю: «вам», «у вас», «вы высоко оценили …». Запрещены слова и конструкции: пользователь, пользователя, пользователю, для пользователя, интересы пользователя, этот пользователь, данный пользователь.\n");
        prompt.append("15. Не повторяй в reason название фильма-кандидата из его строки (id=…): нельзя писать, что он похож на себя, «в духе» себя или что «любимый фильм» с тем же названием — это бессмыслица. Сравнивай только с другими фильмами из профиля, если они есть.\n");
        prompt.append("16. Если другого фильма из профиля для связки нет — укажи только жанр/режиссёра/атмосферу кандидата в связке с любимыми жанрами из профиля, без выдуманных сравнений.\n");
        prompt.append("17. Не пиши общих фраз про «предпочтения» без конкретики; одна ясная мысль, без воды.\n");
        prompt.append("18. Если в профиле указано не рекомендовать режиссёров — не включай их фильмы в recommendedIds.\n\n");

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
                .model(ModelName.GIGA_CHAT)
                .messages(List.of(message))
                .build();

        return gigaChatClient.completions(request);
    }

    private AiRecommendationRankingResult parseRecommendationResponse(String responseContent, int limit) {
        if (responseContent == null || responseContent.isBlank()) {
            return AiRecommendationRankingResult.empty();
        }

        String normalizedContent = normalizeAiJsonPayload(responseContent);
        String jsonPayload = repairMalformedRecommendationJson(isolateFirstJsonObject(normalizedContent));

        AiRecommendationRankingResult fullParse = tryParseRecommendationJsonTree(jsonPayload, limit);
        if (!fullParse.recommendedIds().isEmpty()) {
            return fullParse;
        }

        String minimalJson = buildMinimalRecommendationJson(jsonPayload);
        if (minimalJson != null) {
            try {
                JsonNode root = LENIENT_AI_JSON.readTree(minimalJson);
                List<Long> jsonIds = extractIdsFromJsonNode(root.get("recommendedIds"), limit);
                if (!jsonIds.isEmpty()) {
                    Map<Long, String> explanations = tryExtractExplanationsLenient(jsonPayload, jsonIds);
                    return new AiRecommendationRankingResult(jsonIds, explanations);
                }
            } catch (Exception e) {
                log.debug("Парсинг усечённого JSON рекомендаций не удался: {}", e.getMessage());
            }
        }

        List<Long> sliceIds = extractRecommendedIdsFromArraySlice(jsonPayload, limit);
        if (!sliceIds.isEmpty()) {
            Map<Long, String> explanations = tryExtractExplanationsLenient(jsonPayload, sliceIds);
            return new AiRecommendationRankingResult(sliceIds, explanations);
        }

        List<Long> regexIds = tryExtractRecommendedIdsRegexLegacy(normalizedContent, limit);
        if (!regexIds.isEmpty()) {
            Map<Long, String> explanations = tryExtractExplanationsLenient(normalizedContent, regexIds);
            return new AiRecommendationRankingResult(regexIds, explanations);
        }

        LinkedHashSet<Long> fallbackIds = new LinkedHashSet<>();
        Matcher matcher = NUMBER_PATTERN.matcher(normalizedContent);
        while (matcher.find() && fallbackIds.size() < limit) {
            fallbackIds.add(Long.parseLong(matcher.group()));
        }

        if (fallbackIds.isEmpty()) {
            return AiRecommendationRankingResult.empty();
        }
        log.warn("Рекомендации AI: взят грубый fallback по числам в ответе, порядок может быть неточным");
        return new AiRecommendationRankingResult(List.copyOf(fallbackIds), Map.of());
    }

    private AiRecommendationRankingResult tryParseRecommendationJsonTree(String json, int limit) {
        try {
            JsonNode root = LENIENT_AI_JSON.readTree(json);
            List<Long> jsonIds = extractIdsFromJsonNode(root.get("recommendedIds"), limit);
            if (jsonIds.isEmpty()) {
                return AiRecommendationRankingResult.empty();
            }
            Map<Long, String> explanations = extractExplanationsFromJson(root.get("explanations"), jsonIds);
            return new AiRecommendationRankingResult(jsonIds, explanations);
        } catch (Exception e) {
            log.debug("Полный парсинг JSON рекомендаций не удался (часто из-за невалидного поля reason): {}", e.getMessage());
            return AiRecommendationRankingResult.empty();
        }
    }

    /**
     * Типичные опечатки модели в recommendedIds: закрытие массива ")" вместо "]".
     */
    private static String repairMalformedRecommendationJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        return fixRecommendedIdsArrayClosingParen(json);
    }

    private static String fixRecommendedIdsArrayClosingParen(String json) {
        int idx = json.indexOf("\"recommendedIds\"");
        if (idx < 0) {
            return json;
        }
        int lb = json.indexOf('[', idx);
        if (lb < 0) {
            return json;
        }
        for (int i = lb + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ']') {
                return json;
            }
            if (c == ')') {
                return json.substring(0, i) + ']' + json.substring(i + 1);
            }
            if (!(Character.isDigit(c) || c == ',' || c == ' ' || c == '\n' || c == '\t')) {
                break;
            }
        }
        return json;
    }

    /**
     * Оставляем только валидный блок recommendedIds + пустые explanations, если полный объект битый.
     */
    private static String buildMinimalRecommendationJson(String json) {
        int key = json.indexOf("\"recommendedIds\"");
        if (key < 0) {
            return null;
        }
        int lb = json.indexOf('[', key);
        if (lb < 0) {
            return null;
        }
        int depth = 0;
        for (int i = lb; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return "{\"recommendedIds\":" + json.substring(lb, i + 1) + ",\"explanations\":[]}";
                }
            }
        }
        return null;
    }

    private static List<Long> extractRecommendedIdsFromArraySlice(String json, int limit) {
        String inner = extractRecommendedIdsArrayInner(json);
        if (inner == null || inner.isBlank()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        Matcher nums = NUMBER_PATTERN.matcher(inner);
        while (nums.find() && ids.size() < limit) {
            try {
                ids.add(Long.parseLong(nums.group()));
            } catch (NumberFormatException ignored) {
                // пропускаем
            }
        }
        return ids.isEmpty() ? List.of() : List.copyOf(ids);
    }

    private static String extractRecommendedIdsArrayInner(String json) {
        String fixed = repairMalformedRecommendationJson(json);
        int idx = fixed.indexOf("\"recommendedIds\"");
        if (idx < 0) {
            return null;
        }
        int lb = fixed.indexOf('[', idx);
        if (lb < 0) {
            return null;
        }
        for (int i = lb + 1; i < fixed.length(); i++) {
            char c = fixed.charAt(i);
            if (c == ']' || c == ')') {
                return fixed.substring(lb + 1, i);
            }
        }
        return null;
    }

    private static Map<Long, String> tryExtractExplanationsLenient(String content, List<Long> allowedIds) {
        if (content == null || content.isBlank() || allowedIds == null || allowedIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> allowed = new HashSet<>(allowedIds);
        LinkedHashMap<Long, String> byId = new LinkedHashMap<>();
        Matcher m = EXPL_MOVIE_REASON.matcher(content);
        while (m.find()) {
            long id;
            try {
                id = Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                continue;
            }
            if (!allowed.contains(id)) {
                continue;
            }
            String reason = m.group(2)
                    .replace("\\\"", "\"")
                    .replace("\\n", " ")
                    .replace('\n', ' ')
                    .trim();
            if (reason.isBlank()) {
                continue;
            }
            byId.putIfAbsent(id, truncate(reason, MAX_RECOMMENDATION_REASON_LENGTH));
        }
        return byId.isEmpty() ? Map.of() : Map.copyOf(byId);
    }

    private static List<Long> tryExtractRecommendedIdsRegexLegacy(String text, int limit) {
        Matcher block = RECOMMENDED_IDS_BLOCK.matcher(text);
        if (!block.find()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        Matcher nums = NUMBER_PATTERN.matcher(block.group(1));
        while (nums.find() && ids.size() < limit) {
            try {
                ids.add(Long.parseLong(nums.group()));
            } catch (NumberFormatException ignored) {
                // пропускаем нечисловые фрагменты
            }
        }
        return ids.isEmpty() ? List.of() : List.copyOf(ids);
    }

    private static String normalizeAiJsonPayload(String responseContent) {
        String s = responseContent.stripLeading();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1).stripLeading();
        }
        s = s.replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
        return s.replace('\u201C', '"').replace('\u201D', '"');
    }

    private static String isolateFirstJsonObject(String content) {
        int start = content.indexOf('{');
        if (start < 0) {
            return content;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }
        return content.substring(start);
    }

    private static Map<Long, String> extractExplanationsFromJson(JsonNode explanationsNode, List<Long> orderedRecommendedIds) {
        if (explanationsNode == null || !explanationsNode.isArray() || orderedRecommendedIds.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<Long, String> byId = new LinkedHashMap<>();
        for (JsonNode item : explanationsNode) {
            if (item == null || !item.isObject()) {
                continue;
            }
            Long movieId = readLongId(item.get("movieId"));
            if (movieId == null) {
                movieId = readLongId(item.get("id"));
            }
            if (movieId == null || !orderedRecommendedIds.contains(movieId)) {
                continue;
            }
            String reason = readReasonText(item);
            if (reason.isBlank()) {
                continue;
            }
            byId.putIfAbsent(movieId, truncate(reason, MAX_RECOMMENDATION_REASON_LENGTH));
        }

        return byId.isEmpty() ? Map.of() : Map.copyOf(byId);
    }

    private static Long readLongId(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.canConvertToLong()) {
            return node.longValue();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String readReasonText(JsonNode item) {
        if (item.hasNonNull("reason") && item.get("reason").isTextual()) {
            return item.get("reason").asText().trim();
        }
        if (item.hasNonNull("text") && item.get("text").isTextual()) {
            return item.get("text").asText().trim();
        }
        return "";
    }

    private static List<Long> extractIdsFromJsonNode(JsonNode idsNode, int limit) {
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
