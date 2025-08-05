package com.viewTrack.service.impl;

import com.viewTrack.data.entity.*;
import com.viewTrack.data.repository.DirectorRepository;
import com.viewTrack.data.repository.GenreRepository;
import com.viewTrack.data.repository.ImageRepository;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.dto.request.MovieRequestDto;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final GenreRepository genreRepository;

    private final ImageRepository imageRepository;

    private final MovieRepository movieRepository;

    private final DirectorRepository directorRepository;

    @Override
    public Movie createMovie(MovieRequestDto movieDto) {
        Set<Genre> genres = movieDto.getGenres().stream()
                .map(genreRepository::findByGenreName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        Image poster = movieDto.getPoster() != null ?
                imageRepository.findById(movieDto.getPoster()).orElse(null) : null;

        Set<Director> directors = movieDto.getDirectors().stream()
                .map(directorRepository::findByFullName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        List<Review> reviews = new ArrayList<>();

        Movie movie = Movie.builder()
                .title(movieDto.getTitle())
                .genres(genres)
                .releaseDate(movieDto.getReleaseDate())
                .description(movieDto.getDescription())
                .poster(poster)
                .durationMin(movieDto.getDurationMin())
                .directors(directors)
                .reviews(reviews)
                .build();

        return movieRepository.save(movie);
    }

    @Override
    public List<Movie> getAllMovies() {
        return new ArrayList<>(movieRepository.findAll());
    }

    @Override
    public List<Movie> getMovies(String sort, String genre, String year, String search) {
        List<Movie> movies = movieRepository.findAllWithGenres();

        if (genre != null && !genre.isEmpty()) {
            movies = movies.stream()
                    .filter(movie -> movie.getGenres().stream()
                            .anyMatch(g -> g.getGenreName().equals(genre)))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.isEmpty()) {
            String searchTerm = search.toLowerCase();
            movies = movies.stream()
                    .filter(movie -> movie.getTitle().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
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

        if ("releaseDate".equals(sort)) {
            movies.sort((a, b) -> b.getReleaseDate().compareTo(a.getReleaseDate()));
        } else if ("rating".equals(sort)) {
            movies.sort((a, b) -> {
                float avgRatingA = a.getAverageRating();
                float avgRatingB = b.getAverageRating();

                return Float.compare(avgRatingB, avgRatingA);
            });
        } else {
            movies.sort(Comparator.comparing(Movie::getTitle));
        }

        return movies;
    }

    @Override
    public Movie getMovieById(Long id) {
        return movieRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Фильм не найден"));
    }
}
