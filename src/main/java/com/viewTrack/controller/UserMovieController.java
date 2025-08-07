package com.viewTrack.controller;

import com.viewTrack.data.entity.UserMovie;
import com.viewTrack.dto.request.UserMovieRequest;
import com.viewTrack.service.UserMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movie")
@RequiredArgsConstructor
public class UserMovieController {

    private final UserMovieService userMovieService;

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

    @PostMapping("/watched")
    public ResponseEntity<UserMovie> markAsWatched(@RequestBody UserMovieRequest request) {
        UserMovie userMovie = userMovieService.markAsWatched(request.getMovieId());
        return ResponseEntity.ok(userMovie);
    }

    @DeleteMapping("/watched/{movieId}")
    public ResponseEntity<Void> removeFromWatchedList(@PathVariable Long movieId) {
        userMovieService.removeFromWatchlist(movieId);
        return ResponseEntity.ok().build();
    }
}
