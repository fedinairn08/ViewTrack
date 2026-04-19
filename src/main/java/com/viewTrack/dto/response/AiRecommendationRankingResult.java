package com.viewTrack.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Результат AI-ранжирования: порядок id и краткое объяснение по каждому
 */
public record AiRecommendationRankingResult(List<Long> recommendedIds, Map<Long, String> explanationByMovieId) {

    public AiRecommendationRankingResult {
        recommendedIds = recommendedIds == null ? List.of() : List.copyOf(recommendedIds);
        explanationByMovieId = explanationByMovieId == null ? Map.of() : Map.copyOf(explanationByMovieId);
    }

    public static AiRecommendationRankingResult empty() {
        return new AiRecommendationRankingResult(List.of(), Map.of());
    }
}
