package com.viewTrack.service.impl;

import com.viewTrack.data.entity.AiReview;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.repository.AiReviewRepository;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.service.AiReviewService;
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

    private static final int MIN_REVIEWS_FOR_AI_REVIEW = 1;

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
        List<Review> reviews = reviewRepository.findByMovieId(movie.getId())
                .stream()
                .filter(review -> review.getContent() != null)
                .collect(Collectors.toList());


        
        if (reviews.size() < MIN_REVIEWS_FOR_AI_REVIEW) {
            return null;
        }

        String reviewContent = gigaChatService.generateMovieReview(movie, reviews);
        
        AiReview aiReview = AiReview.builder()
                .movie(movie)
                .content(reviewContent)
                .reviewsCountAtGeneration(reviews.size())
                .build();

        return aiReviewRepository.save(aiReview);
    }
}
