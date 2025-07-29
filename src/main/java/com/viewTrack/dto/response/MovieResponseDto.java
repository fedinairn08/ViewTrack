package com.viewTrack.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MovieResponseDto {
    private String title;

    private List<String> genres;

    private LocalDate releaseDate;

    private String description;

    private ImageResponseDto poster;

    private int durationMin;

    private List<String> directors;

    private List<String> reviews;
}
