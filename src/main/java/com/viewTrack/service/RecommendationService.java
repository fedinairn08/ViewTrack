package com.viewTrack.service;

import com.viewTrack.data.entity.User;
import com.viewTrack.dto.response.RecommendationsResult;

public interface RecommendationService {
    RecommendationsResult getRecommendations(User user, int limit);
}
