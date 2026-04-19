package com.viewTrack.service.impl;

import com.viewTrack.data.entity.AiReview;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.repository.AiReviewRepository;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.dto.response.ExternalReviewResponseDto;
import com.viewTrack.service.AiReviewService;
import com.viewTrack.service.ExternalReviewService;
import com.viewTrack.service.GigaChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewServiceImpl implements AiReviewService {

    private final GigaChatService gigaChatService;
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final AiReviewRepository aiReviewRepository;
    private final ExternalReviewService externalReviewService;

    private static final int MAX_KINOPOISK_REVIEWS_FOR_AI = 5;

    @Override
    public AiReview getOrGenerateReviewForMovie(Long movieId) {
        try {
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new RuntimeException("Фильм не найден"));

            Optional<AiReview> existingReview = aiReviewRepository.findByMovie(movie);

            return existingReview.orElseGet(() -> generateAndSaveReview(movie));

        } catch (Exception e) {
            log.error("Ошибка при получении AI-рецензии для фильма {}: {}", movieId, e.getMessage());
            return null;
        }
    }

    @Override
    public void regenerateReviewForMovie(Long movieId) {
        try {
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new RuntimeException("Фильм не найден"));

            aiReviewRepository.findByMovie(movie).ifPresent(aiReviewRepository::delete);

            generateAndSaveReview(movie);

        } catch (Exception e) {
            log.error("Ошибка при перегенерации AI-рецензии для фильма {}: {}", movieId, e.getMessage());
        }
    }

    private AiReview generateAndSaveReview(Movie movie) {
        List<Review> siteReviews = reviewRepository.findByMovieId(movie.getId())
                .stream()
                .filter(review -> review.getContent() != null && !review.getContent().isBlank())
                .collect(Collectors.toList());

        List<ExternalReviewResponseDto> kinopoiskReviews = List.of();
        if (siteReviews.isEmpty() && externalReviewService.isConfigured()) {
            kinopoiskReviews = externalReviewService.getReviewsForMovie(movie).stream()
                    .filter(r -> r.getContent() != null && !r.getContent().isBlank())
                    .limit(MAX_KINOPOISK_REVIEWS_FOR_AI)
                    .toList();
        }

        if (siteReviews.isEmpty() && kinopoiskReviews.isEmpty()) {
            return null;
        }

        String reviewContent = gigaChatService.generateMovieReview(movie, siteReviews, kinopoiskReviews);

        int sourcesCount = siteReviews.size() + kinopoiskReviews.size();

        AiReview aiReview = AiReview.builder()
                .movie(movie)
                .content(reviewContent)
                .reviewsCountAtGeneration(sourcesCount)
                .build();

        return aiReviewRepository.save(aiReview);
    }
}
