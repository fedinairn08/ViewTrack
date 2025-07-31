package com.viewTrack.dto;

import lombok.Data;

@Data
public class RateMovieRequest {
    private Long movieId;
    private int rating;
}
