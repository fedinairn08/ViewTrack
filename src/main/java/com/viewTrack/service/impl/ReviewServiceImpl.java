package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.ReviewService;
import com.viewTrack.service.UserService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final UserService userService;

    private final AuthUtils authUtils;

    private final MovieRepository movieRepository;

    private final ReviewRepository reviewRepository;
    @Override
    public Review rateMovie(Long movieId, int rating) {
        // пока не работает
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
}
