package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.dto.request.MovieRequestDto;

public interface MovieService {
    Movie createMovie(MovieRequestDto movieRequestDto);
}
