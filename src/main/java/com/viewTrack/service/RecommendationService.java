package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.User;

import java.util.List;

public interface RecommendationService {
    List<Movie> getRecommendations(User user, int limit);
}
