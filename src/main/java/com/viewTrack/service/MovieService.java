package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.dto.request.MovieRequestDto;

import java.util.List;

public interface MovieService {
    Movie createMovie(MovieRequestDto movieRequestDto);

    List<Movie> getAllMovies();

    List<Movie> getMovies(String sort, String genre, String year, String search);

    Movie getMovieById(Long id);
}
