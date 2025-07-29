package com.viewTrack.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MovieRequestDto {
    private String title;

    private List<String> genres;

    private LocalDate releaseDate;

    private String description;

    private Long poster;

    private int durationMin;

    private List<String> directors;
}
