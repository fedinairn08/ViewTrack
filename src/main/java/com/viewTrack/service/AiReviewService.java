package com.viewTrack.service;

import com.viewTrack.data.entity.AiReview;

public interface AiReviewService {
    
    AiReview getOrGenerateReviewForMovie(Long movieId);
    
    void regenerateReviewForMovie(Long movieId);
}
