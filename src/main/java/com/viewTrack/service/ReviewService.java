package com.viewTrack.service;

import com.viewTrack.data.entity.Review;

public interface ReviewService {
    Review rateMovie(Long movieId, int rating);

    Review updateReview(Long userId, Long movieId, String content);
}
