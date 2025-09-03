package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.data.repository.UserRepository;
import com.viewTrack.exeption.AccessDeniedException;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.AiReviewService;
import com.viewTrack.service.ReviewService;
import com.viewTrack.service.UserService;
import com.viewTrack.utils.AuthUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final UserService userService;

    private final AuthUtils authUtils;

    private final MovieRepository movieRepository;

    private final ReviewRepository reviewRepository;

    private final UserRepository userRepository;

    private final AiReviewService aiReviewService;

    @Override
    public Review rateMovie(Long movieId, int rating) {
        User currentUser = userService.getById(authUtils.getUserEntity().getId());

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        Review review = reviewRepository.findByUserAndMovie(currentUser, movie)
                .orElseGet(() -> Review.builder()
                        .user(currentUser)
                        .movie(movie)
                        .build());

        review.setRating(rating);

        Review updateReview = reviewRepository.save(review);

        updateMovieAverageRating(movie);

        return updateReview;
    }

    private void updateMovieAverageRating(Movie movie) {
        List<Review> reviews = reviewRepository.findByMovieId(movie.getId());

        List<Review> reviewsWithRating = reviews.stream()
                .filter(review -> review.getRating() > 0)
                .toList();

        if (reviewsWithRating.isEmpty()) {
            movie.setAverageRating(0.0f);
        } else {
            double average = reviewsWithRating.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            movie.setAverageRating((float) average);
        }

        movieRepository.save(movie);
    }

    @Override
    public Review saveReview(Long userId, Long movieId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new EntityNotFoundException("Фильм не найден"));

        Review review = reviewRepository.findByUserAndMovie(user, movie)
                .orElseThrow(() -> new EntityNotFoundException("Отзыв не найден"));

        review.setContent(content);
        review.setUpdatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        notifyReviewChanged(movieId);

        return savedReview;
    }

    @Override
    public Review updateReview(Long userId, Long reviewId, String content) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Отзыв не найден"));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Вы не можете редактировать этот отзыв");
        }

        review.setContent(content);
        review.setUpdatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        notifyReviewChanged(review.getMovie().getId());

        return savedReview;
    }

    @Override
    public Review deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Отзыв не найден"));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Вы не можете удалить этот отзыв");
        }

        Long movieId = review.getMovie().getId();
        review.setContent(null);
        review.setUpdatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        notifyReviewChanged(movieId);

        return savedReview;
    }

    @Override
    public Review removeRating(Long movieId) {
        User currentUser = userService.getById(authUtils.getUserEntity().getId());

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        Review review = reviewRepository.findByUserAndMovie(currentUser, movie)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        reviewRepository.delete(review);

        updateMovieAverageRating(movie);

        notifyReviewChanged(movieId);

        return null;
    }
    
    @Override
    public void notifyReviewChanged(Long movieId) {
        try {
            aiReviewService.regenerateReviewForMovie(movieId);
        } catch (Exception e) {
            log.error("Ошибка при обновлении AI-рецензии для фильма {}: {}", movieId, e.getMessage());
        }
    }
}
