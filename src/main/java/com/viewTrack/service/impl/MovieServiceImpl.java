package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Director;
import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Image;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.repository.DirectorRepository;
import com.viewTrack.data.repository.GenreRepository;
import com.viewTrack.data.repository.ImageRepository;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.s3storage.S3File;
import com.viewTrack.service.ImageService;
import com.viewTrack.service.MovieService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final GenreRepository genreRepository;

    private final ImageRepository imageRepository;

    private final MovieRepository movieRepository;

    private final DirectorRepository directorRepository;

    private final ImageService imageService;

    @Override
    public List<Movie> getAllMovies() {
        return new ArrayList<>(movieRepository.findAll());
    }

    @Override
    public List<Movie> getMovies(String sort, String genre, String year, String search, Long directorId) {
        List<Movie> movies = movieRepository.findAllWithGenresAndDirectors();

        if (genre != null && !genre.isEmpty()) {
            movies = movies.stream()
                    .filter(movie -> movie.getGenres().stream()
                            .anyMatch(g -> g.getGenreName().equals(genre)))
                    .collect(Collectors.toList());
        }

        if (directorId != null) {
            movies = movies.stream()
                    .filter(movie -> movie.getDirectors().stream()
                            .anyMatch(d -> d.getId().equals(directorId)))
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

    @Override
    public Movie addMovie(String title, String description, LocalDate releaseDate, List<Long> genreIds, List<Long> directorIds, MultipartFile poster) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setReleaseDate(releaseDate);
        movie.setAverageRating(0.0f);

        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> genres = genreRepository.findAllById(genreIds);
            movie.setGenres(new HashSet<>(genres));
        }

        if (directorIds != null && !directorIds.isEmpty()) {
            List<Director> directors = directorRepository.findAllById(directorIds);
            movie.setDirectors(new HashSet<>(directors));
        }

        uploadPoster(movie, poster);

        return movieRepository.save(movie);
    }

    @Override
    public Movie updateMovie(Long id, String title, String description, LocalDate releaseDate,
                             List<Long> genreIds, List<Long> directorIds, MultipartFile poster) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Фильм не найден"));

        movie.setTitle(title);
        movie.setDescription(description);
        movie.setReleaseDate(releaseDate);

        if (genreIds != null) {
            if (genreIds.isEmpty()) {
                movie.getGenres().clear();
            } else {
                List<Genre> genres = genreRepository.findAllById(genreIds);
                movie.setGenres(new HashSet<>(genres));
            }
        }

        if (directorIds != null) {
            if (directorIds.isEmpty()) {
                movie.getDirectors().clear();
            } else {
                List<Director> directors = directorRepository.findAllById(directorIds);
                movie.setDirectors(new HashSet<>(directors));
            }
        }

        if (poster != null && !poster.isEmpty()) {
            uploadPoster(movie, poster);
        }

        return movieRepository.save(movie);
    }

    public void uploadPoster(Movie movie, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Поддерживаются только изображения");
        }

        S3File s3File = imageService.upload(file);

        Image image = Image.builder()
                .filename(s3File.getFilename())
                .uploadId(s3File.getUploadId())
                .build();

        Image savedImage = imageRepository.save(image);

        if (movie.getPoster() != null) {
            Image oldImage = movie.getPoster();

            imageService.delete(oldImage.getFilename());

            movie.setPoster(null);

            imageRepository.delete(oldImage);
        }

        movie.setPoster(savedImage);
    }
}
