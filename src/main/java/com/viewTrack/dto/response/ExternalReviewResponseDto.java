package com.viewTrack.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ExternalReviewResponseDto {

    private final String author;

    private final String content;

    private final Double rating;

    private final String sourceUrl;

    private final LocalDateTime createdAt;
}
