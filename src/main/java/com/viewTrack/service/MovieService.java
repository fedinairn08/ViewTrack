package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface MovieService {
    List<Movie> getAllMovies();

    List<Movie> getMovies(String sort, String genre, String year, String search, Long directorId);

    Movie getMovieById(Long id);

    Movie addMovie(String title, String description, LocalDate releaseDate, List<Long> genreIds,
                   List<Long> directorIds, MultipartFile poster);

    Movie updateMovie(Long id, String title, String description, LocalDate releaseDate, List<Long> genreIds,
                      List<Long> directorIds, MultipartFile poster, String deletePoster);

    void deleteMovie(Long id);
    
    List<Movie> getMoviesByDirector(Long directorId);
}
