package com.viewTrack.dto.request;

import lombok.Data;

@Data
public class SaveReviewRequest {
    private Long movieId;
    private String content;
}
