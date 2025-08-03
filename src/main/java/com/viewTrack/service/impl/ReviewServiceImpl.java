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

        return reviewRepository.save(review);
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
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Отзыв не найден"));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Вы не можете удалить этот отзыв");
        }

        reviewRepository.delete(review);
    }
}
