package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.data.repository.UserRepository;
import com.viewTrack.exeption.AccessDeniedException;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.ReviewService;
import com.viewTrack.service.UserService;
import com.viewTrack.utils.AuthUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final UserService userService;

    private final AuthUtils authUtils;

    private final MovieRepository movieRepository;

    private final ReviewRepository reviewRepository;

    private final UserRepository userRepository;

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

        return reviewRepository.save(review);
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

        return reviewRepository.save(review);
    }

    @Override
    public Review deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Отзыв не найден"));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Вы не можете удалить этот отзыв");
        }

        review.setContent(null);
        review.setUpdatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
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

        return null;
    }
}
