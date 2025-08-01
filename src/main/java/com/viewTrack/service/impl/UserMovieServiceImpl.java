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

import java.time.LocalDate;
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
    public List<Movie> getToWatchList(User user, String sort, String genre, String year) {
        List<UserMovie> userMovies = userMovieRepository.findByUserAndType(user, Type.TO_WATCH);
        List<Movie> movies = userMovies.stream()
                .map(UserMovie::getMovie)
                .collect(Collectors.toList());

        if (genre != null && !genre.isEmpty()) {
            movies = movies.stream()
                    .filter(movie -> movie.getGenres().stream()
                            .anyMatch(g -> g.getGenreName().equals(genre)))
                    .collect(Collectors.toList());
        }

        if ("releaseDate".equals(sort)) {
            movies.sort((a, b) -> b.getReleaseDate().compareTo(a.getReleaseDate()));
        }


        if (year != null && !year.isEmpty()) {
            int currentYear = LocalDate.now().getYear();
            movies = movies.stream().filter(movie -> {
                Integer releaseYear = movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null;
                if (releaseYear == null) return false;

                return switch (year) {
                    case "2025" -> releaseYear == 2025;
                    case "2024" -> releaseYear == 2024;
                    case "2023" -> releaseYear == 2023;
                    case "2022" -> releaseYear == 2022;
                    case "2021" -> releaseYear == 2021;
                    case "2020" -> releaseYear == 2020;
                    case "2010-2019" -> releaseYear >= 2010 && releaseYear <= 2019;
                    case "2000-2009" -> releaseYear >= 2000 && releaseYear <= 2009;
                    case "1990-1999" -> releaseYear >= 1990 && releaseYear <= 1999;
                    case "1980-1989" -> releaseYear >= 1980 && releaseYear <= 1989;
                    case "before1980" -> releaseYear < 1980;
                    default -> true;
                };
            }).collect(Collectors.toList());
        }

        return movies;
    }

    @Override
    public List<Movie> getWatchedList(User user, String sort, String genre, String year) {
        List<UserMovie> userMovies = userMovieRepository.findByUserAndType(user, Type.WATCHED);
        List<Movie> movies = userMovies.stream()
                .map(UserMovie::getMovie)
                .collect(Collectors.toList());

        if (genre != null && !genre.isEmpty()) {
            movies = movies.stream()
                    .filter(movie -> movie.getGenres().stream()
                            .anyMatch(g -> g.getGenreName().equals(genre)))
                    .collect(Collectors.toList());
        }

        if ("releaseDate".equals(sort)) {
            movies.sort((a, b) -> b.getReleaseDate().compareTo(a.getReleaseDate()));
        }


        if (year != null && !year.isEmpty()) {
            int currentYear = LocalDate.now().getYear();
            movies = movies.stream().filter(movie -> {
                Integer releaseYear = movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null;
                if (releaseYear == null) return false;

                return switch (year) {
                    case "2025" -> releaseYear == 2025;
                    case "2024" -> releaseYear == 2024;
                    case "2023" -> releaseYear == 2023;
                    case "2022" -> releaseYear == 2022;
                    case "2021" -> releaseYear == 2021;
                    case "2020" -> releaseYear == 2020;
                    case "2010-2019" -> releaseYear >= 2010 && releaseYear <= 2019;
                    case "2000-2009" -> releaseYear >= 2000 && releaseYear <= 2009;
                    case "1990-1999" -> releaseYear >= 1990 && releaseYear <= 1999;
                    case "1980-1989" -> releaseYear >= 1980 && releaseYear <= 1989;
                    case "before1980" -> releaseYear < 1980;
                    default -> true;
                };
            }).collect(Collectors.toList());
        }

        return movies;
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

    @Override
    public UserMovie markAsWatched(Long movieId) {
        User currentUser = authUtils.getUserEntity();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Фильм не найден"));

        UserMovie userMovie = userMovieRepository.findByUserAndMovie(currentUser, movie)
                .orElseGet(() -> UserMovie.builder()
                        .user(currentUser)
                        .movie(movie)
                        .build());

        userMovie.setType(Type.WATCHED);

        if (Type.TO_WATCH.equals(userMovie.getType())) {
            userMovieRepository.delete(userMovie);
        }

        return userMovieRepository.save(userMovie);
    }

    @Override
    public boolean isInWatchedList(Long movieId) {
        User currentUser = authUtils.getUserEntity();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Фильм не найден"));

        return userMovieRepository.findByUserAndMovie(currentUser, movie)
                .map(userMovie -> Type.WATCHED.equals(userMovie.getType()))
                .orElse(false);
    }
}
