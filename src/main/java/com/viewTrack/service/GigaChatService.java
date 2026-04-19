package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.dto.response.AiRecommendationRankingResult;
import com.viewTrack.dto.response.ExternalReviewResponseDto;

import java.util.List;

public interface GigaChatService {

    String generateMovieReview(Movie movie,
                               List<Review> siteReviews,
                               List<ExternalReviewResponseDto> kinopoiskReviews);

    AiRecommendationRankingResult rankMovieRecommendations(String userTasteProfile, List<Movie> candidateMovies, int limit);
}
