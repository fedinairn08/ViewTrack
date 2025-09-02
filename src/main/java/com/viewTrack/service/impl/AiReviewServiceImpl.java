package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.service.AiReviewService;
import com.viewTrack.service.GigaChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewServiceImpl implements AiReviewService {

    private final GigaChatService gigaChatService;
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;

    private static final int MIN_REVIEWS_FOR_AI_REVIEW = 1;

    @Override
    public String generateReviewForMovie(Long movieId) {
        try {
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new RuntimeException("Фильм не найден"));

            List<Review> reviews = reviewRepository.findByMovieId(movieId);
            
            if (reviews.size() < MIN_REVIEWS_FOR_AI_REVIEW) {
                return "Недостаточно отзывов для генерации рецензии. Нужно минимум " + 
                       MIN_REVIEWS_FOR_AI_REVIEW + " отзыва.";
            }

            return gigaChatService.generateMovieReview(movie, reviews);

        } catch (Exception e) {
            return "Произошла ошибка при генерации рецензии";
        }
    }
}
