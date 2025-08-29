package com.viewTrack.service.impl;

import com.viewTrack.data.entity.AbstractEntity;
import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.repository.GenreRepository;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.service.GenreService;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final MovieRepository movieRepository;

    @Override
    public Genre createGenre(String genreName) {
        Genre genre = new Genre();
        genre.setGenreName(genreName.trim());
        return genreRepository.save(genre);
    }

    @Override
    public void deleteGenre(long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new EntityExistsException("Жанр не найден"));

        List<Movie> moviesWithGenre = movieRepository.findAll().stream()
                .filter(movie -> movie.getGenres().contains(genre))
                .toList();

        for (Movie movie : moviesWithGenre) {
            movie.getGenres().remove(genre);
            movieRepository.save(movie);
        }

        genreRepository.deleteById(id);
    }

    @Override
    public List<Genre> findAll() {
        return genreRepository.findAll();
    }

    @Override
    public List<Genre> getGenres(String sort, String search) {
        List<Genre> genres = genreRepository.findAll();

        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.trim().toLowerCase();
            genres = genres.stream()
                    .filter(genre -> genre.getGenreName().toLowerCase().contains(searchTerm))
                    .toList();
        }

        if (sort != null && !sort.trim().isEmpty()) {
            if (sort.equals("name")) {
                genres = genres.stream()
                        .sorted((g1, g2) -> g1.getGenreName().compareToIgnoreCase(g2.getGenreName()))
                        .toList();
            } else {
                genres = genres.stream()
                        .sorted(Comparator.comparing(AbstractEntity::getId))
                        .toList();
            }
        } else {
            genres = genres.stream()
                    .sorted(Comparator.comparing(AbstractEntity::getId))
                    .toList();
        }
        
        return genres;
    }

    @Override
    public boolean existsByGenreName(String genreName) {
        return genreRepository.existsByGenreName(genreName);
    }
}
