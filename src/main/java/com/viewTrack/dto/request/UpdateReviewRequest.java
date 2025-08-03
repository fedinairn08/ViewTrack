package com.viewTrack.dto.request;

import lombok.Data;

@Data
public class UpdateReviewRequest {
    private Long movieId;
    private String content;
}
