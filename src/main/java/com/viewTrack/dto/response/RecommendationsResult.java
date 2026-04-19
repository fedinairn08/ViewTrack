package com.viewTrack.dto.response;

import com.viewTrack.data.entity.Movie;

import java.util.List;
import java.util.Map;

public record RecommendationsResult(List<Movie> movies, Map<Long, String> aiExplanationByMovieId) {

    public RecommendationsResult {
        movies = movies == null ? List.of() : List.copyOf(movies);
        aiExplanationByMovieId = aiExplanationByMovieId == null ? Map.of() : Map.copyOf(aiExplanationByMovieId);
    }

    public static RecommendationsResult ofMoviesOnly(List<Movie> movies) {
        return new RecommendationsResult(movies, Map.of());
    }
}
