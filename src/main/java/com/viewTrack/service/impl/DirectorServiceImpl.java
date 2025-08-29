package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Director;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.repository.DirectorRepository;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.service.DirectorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorServiceImpl implements DirectorService {

    private final DirectorRepository directorRepository;
    private final MovieRepository movieRepository;

    @Override
    public List<Director> findAll() {
        return directorRepository.findAll();
    }

    @Override
    public List<Director> getDirectors(String sort, String search) {
        List<Director> directors = directorRepository.findAll();

        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.trim().toLowerCase();
            directors = directors.stream()
                    .filter(director -> director.getFullName().toLowerCase().contains(searchTerm))
                    .toList();
        }

        if (sort != null && !sort.trim().isEmpty()) {
            switch (sort) {
                case "nameDesc":
                    directors = directors.stream()
                            .sorted((d1, d2) -> d2.getFullName().compareToIgnoreCase(d1.getFullName()))
                            .toList();
                    break;
                case "birthDate":
                case "birthDateAsc":
                    boolean ascending = "birthDateAsc".equals(sort);
                    directors = directors.stream()
                            .sorted((d1, d2) -> {
                                if (d1.getBirthDate() == null && d2.getBirthDate() == null) {
                                    return 0;
                                }
                                if (d1.getBirthDate() == null) {
                                    return 1;
                                }
                                if (d2.getBirthDate() == null) {
                                    return -1;
                                }
                                return ascending ? 
                                    d1.getBirthDate().compareTo(d2.getBirthDate()) : 
                                    d2.getBirthDate().compareTo(d1.getBirthDate());
                            })
                            .toList();
                    break;
                default:
                    directors = directors.stream()
                            .sorted((d1, d2) -> d1.getFullName().compareToIgnoreCase(d2.getFullName()))
                            .toList();
                    break;
            }
        } else {
            directors = directors.stream()
                    .sorted((d1, d2) -> d1.getFullName().compareToIgnoreCase(d2.getFullName()))
                    .toList();
        }
        
        return directors;
    }

    @Override
    public Director createDirector(String fullName, String birthDate) {
        Director director = new Director();
        director.setFullName(fullName.trim());
        
        if (birthDate != null && !birthDate.trim().isEmpty()) {
            try {
                LocalDate parsedDate = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                director.setBirthDate(parsedDate);
            } catch (Exception e) {
            }
        }
        
        return directorRepository.save(director);
    }

    @Override
    public void deleteDirector(Long id) {
        Director director = directorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Режиссер не найден"));

        List<Movie> moviesWithDirector = movieRepository.findAll().stream()
                .filter(movie -> movie.getDirectors().contains(director))
                .toList();

        for (Movie movie : moviesWithDirector) {
            movie.getDirectors().remove(director);
            movieRepository.save(movie);
        }

        directorRepository.deleteById(id);
    }
}
