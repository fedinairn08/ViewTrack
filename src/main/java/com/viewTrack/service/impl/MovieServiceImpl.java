package com.viewTrack.service.impl;

import com.viewTrack.data.entity.*;
import com.viewTrack.data.repository.DirectorRepository;
import com.viewTrack.data.repository.GenreRepository;
import com.viewTrack.data.repository.ImageRepository;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.dto.request.MovieRequestDto;
import com.viewTrack.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final GenreRepository genreRepository;

    private final ImageRepository imageRepository;

    private final MovieRepository movieRepository;

    private final DirectorRepository directorRepository;

    @Override
    public Movie createMovie(MovieRequestDto movieDto) {
        List<Genre> genres = movieDto.getGenres().stream()
                .map(genreRepository::findByGenreName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        Image poster = movieDto.getPoster() != null ?
                imageRepository.findById(movieDto.getPoster()).orElse(null) : null;

        List<Director> directors = movieDto.getDirectors().stream()
                .map(directorRepository::findByFullName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

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
}
