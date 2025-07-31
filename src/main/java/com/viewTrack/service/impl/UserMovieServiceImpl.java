package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.entity.UserMovie;
import com.viewTrack.data.enums.Type;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.UserMovieRepository;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.UserMovieService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMovieServiceImpl implements UserMovieService {

    private final AuthUtils authUtils;

    private final MovieRepository movieRepository;

    private final UserMovieRepository userMovieRepository;

    @Override
    public UserMovie addToWatchlist(Long movieId) {
        User currentUser = authUtils.getUserEntity();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Фильм не найден"));

        Optional<UserMovie> existing = userMovieRepository.findByUserAndMovie(currentUser, movie);
        UserMovie userMovie = existing.orElseGet(() -> UserMovie.builder()
                .user(currentUser)
                .movie(movie)
                .build());

        userMovie.setType(Type.TO_WATCH);
        return userMovieRepository.save(userMovie);
    }

    @Override
    public void removeFromWatchlist(Long movieId) {
        User currentUser = authUtils.getUserEntity();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Фильм не найден"));

        userMovieRepository.findByUserAndMovie(currentUser, movie)
                .ifPresent(userMovieRepository::delete);

    }

    @Override
    public List<Movie> getToWatchList() {
        User currentUser = authUtils.getUserEntity();
        return userMovieRepository.findByUserAndType(currentUser, Type.TO_WATCH)
                .stream()
                .map(UserMovie::getMovie)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isInToWatchList(Long movieId) {
        User currentUser = authUtils.getUserEntity();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Фильм не найден"));

        return userMovieRepository.findByUserAndMovie(currentUser, movie)
                .map(userMovie -> Type.TO_WATCH.equals(userMovie.getType()))
                .orElse(false);
    }
}
