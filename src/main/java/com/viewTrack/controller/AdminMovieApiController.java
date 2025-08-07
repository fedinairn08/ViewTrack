package com.viewTrack.controller;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class AdminMovieApiController {

    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<Movie> addMovie(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
            @RequestParam(required = false) List<Long> genreIds,
            @RequestParam(required = false) List<Long> directorIds,
            @RequestParam(required = false) MultipartFile poster) {

        Movie movie = movieService.addMovie(title, description, releaseDate, genreIds, directorIds, poster);
        return ResponseEntity.ok(movie);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movie> updateMovie(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
            @RequestParam(required = false) List<Long> genreIds,
            @RequestParam(required = false) List<Long> directorIds,
            @RequestParam(required = false) MultipartFile poster) {

        Movie movie = movieService.updateMovie(id, title, description, releaseDate, genreIds, directorIds, poster);
        return ResponseEntity.ok(movie);
    }
}
