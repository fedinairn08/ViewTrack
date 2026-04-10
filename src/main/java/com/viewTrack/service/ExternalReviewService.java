package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.dto.response.ExternalReviewResponseDto;

import java.util.List;

public interface ExternalReviewService {

    List<ExternalReviewResponseDto> getReviewsForMovie(Movie movie);

    boolean isConfigured();

    String getProviderName();
}
