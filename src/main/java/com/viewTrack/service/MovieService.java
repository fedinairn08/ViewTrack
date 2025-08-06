package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.dto.request.MovieRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface MovieService {
    Movie createMovie(MovieRequestDto movieRequestDto);

    List<Movie> getAllMovies();

    List<Movie> getMovies(String sort, String genre, String year, String search, Long directorId);

    Movie getMovieById(Long id);

    Movie addMovie(String title, String description, LocalDate releaseDate, List<Long> genreIds,
                   List<Long> directorIds, MultipartFile poster);
}
