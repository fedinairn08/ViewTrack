package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.UserMovie;

import java.util.List;

public interface UserMovieService {
    UserMovie addToWatchlist(Long movieId);

    void removeFromWatchlist(Long movieId);

    List<Movie> getToWatchList();

    boolean isInToWatchList(Long movieId);
}
