package com.viewTrack.controller;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.UserMovie;
import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.dto.request.MovieRequestDto;
import com.viewTrack.dto.request.UserMovieRequest;
import com.viewTrack.dto.response.MovieResponseDto;
import com.viewTrack.mapper.MovieMapper;
import com.viewTrack.service.MovieService;
import com.viewTrack.service.UserMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movie")
@RequiredArgsConstructor
public class UserMovieController {

    private final MovieService movieService;

    private final MovieMapper movieMapper;

    private final UserMovieService userMovieService;

    @PostMapping("/add")
    public ResponseEntity<BasicApiResponse<MovieResponseDto>> createMovie(@RequestBody MovieRequestDto movieDto) {
        Movie movie = movieService.createMovie(movieDto);
        MovieResponseDto movieResponseDto = movieMapper.toMovieResponseDto(movie);
        return ResponseEntity.ok(new BasicApiResponse<>(movieResponseDto));
    }

    @PostMapping("/to-watch")
    public ResponseEntity<UserMovie> addToWatchlist(@RequestBody UserMovieRequest request) {
        UserMovie userMovie = userMovieService.addToWatchlist(request.getMovieId());
        return ResponseEntity.ok(userMovie);
    }

    @DeleteMapping("/to-watch/{movieId}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable Long movieId) {
        userMovieService.removeFromWatchlist(movieId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/to-watch")
    public ResponseEntity<List<Movie>> getToWatchList() {
        List<Movie> movies = userMovieService.getToWatchList();
        return ResponseEntity.ok(movies);
    }
}
