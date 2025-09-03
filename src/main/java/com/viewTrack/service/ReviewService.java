package com.viewTrack.service;

import com.viewTrack.data.entity.Review;

public interface ReviewService {
    Review rateMovie(Long movieId, int rating);

    Review saveReview(Long userId, Long movieId, String content);

    Review updateReview(Long userId, Long reviewId, String content);

    Review deleteReview(Long userId, Long reviewId);

    Review removeRating(Long movieId);

    void notifyReviewChanged(Long movieId);
}
