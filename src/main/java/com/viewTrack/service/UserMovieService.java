package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.entity.UserMovie;

import java.util.List;

public interface UserMovieService {
    UserMovie addToWatchlist(Long movieId);

    void removeFromWatchlist(Long movieId);

    List<Movie> getToWatchList(User user, String sort, String genre, String year, String search, Long directorId);

    List<Movie> getWatchedList(User user, String sort, String genre, String year, String search, Long directorId);

    boolean isInToWatchList(Long movieId);

    UserMovie markAsWatched(Long movieId);

    boolean isInWatchedList(Long movieId);
}
