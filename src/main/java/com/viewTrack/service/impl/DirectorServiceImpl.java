package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Director;
import com.viewTrack.data.entity.Image;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.repository.DirectorRepository;
import com.viewTrack.data.repository.ImageRepository;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.s3storage.S3File;
import com.viewTrack.service.DirectorService;
import com.viewTrack.service.ImageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorServiceImpl implements DirectorService {

    private final DirectorRepository directorRepository;
    private final MovieRepository movieRepository;
    private final ImageService imageService;
    private final ImageRepository imageRepository;

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
    public Director getDirectorById(Long id) {
        return directorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Режиссер не найден"));
    }

    @Override
    public Director createDirector(String fullName, String birthDate, String deathDate, MultipartFile photo) {
        Director director = new Director();
        director.setFullName(fullName.trim());
        
        if (birthDate != null && !birthDate.trim().isEmpty()) {
            try {
                LocalDate parsedDate = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                director.setBirthDate(parsedDate);
            } catch (Exception e) {
            }
        }
        
        if (deathDate != null && !deathDate.trim().isEmpty()) {
            try {
                LocalDate parsedDate = LocalDate.parse(deathDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                director.setDeathDate(parsedDate);
            } catch (Exception e) {
            }
        }
        
        if (photo != null && !photo.isEmpty()) {
            uploadPhoto(director, photo);
        }
        
        return directorRepository.save(director);
    }

    @Override
    public Director updateDirector(Long id, String fullName, String birthDate, String deathDate, MultipartFile photo, String deletePhoto) {
        Director director = directorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Режиссер не найден"));

        director.setFullName(fullName.trim());
        
        if (birthDate != null && !birthDate.trim().isEmpty()) {
            try {
                LocalDate parsedDate = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                director.setBirthDate(parsedDate);
            } catch (Exception e) {
                director.setBirthDate(null);
            }
        } else {
            director.setBirthDate(null);
        }
        
        if (deathDate != null && !deathDate.trim().isEmpty()) {
            try {
                LocalDate parsedDate = LocalDate.parse(deathDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                director.setDeathDate(parsedDate);
            } catch (Exception e) {
                director.setDeathDate(null);
            }
        } else {
            director.setDeathDate(null);
        }

        if ("true".equals(deletePhoto)) {
            // Удаляем фотографию
            if (director.getPhoto() != null) {
                Image oldImage = director.getPhoto();
                imageService.delete(oldImage.getFilename());
                director.setPhoto(null);
                imageRepository.delete(oldImage);
            }
        } else if (photo != null && !photo.isEmpty()) {
            if (director.getPhoto() != null) {
                Image oldImage = director.getPhoto();
                imageService.delete(oldImage.getFilename());
                director.setPhoto(null);
                imageRepository.delete(oldImage);
            }
            uploadPhoto(director, photo);
        }
        
        return directorRepository.save(director);
    }

    private void uploadPhoto(Director director, MultipartFile file) {
        try {
            S3File s3File = imageService.upload(file);
            Image image = Image.builder()
                    .filename(s3File.getFilename())
                    .uploadId(s3File.getUploadId())
                    .build();
            image = imageRepository.save(image);
            director.setPhoto(image);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке фотографии", e);
        }
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

        if (director.getPhoto() != null) {
            Image oldImage = director.getPhoto();
            imageService.delete(oldImage.getFilename());
            director.setPhoto(null);
            imageRepository.delete(oldImage);
        }

        directorRepository.deleteById(id);
    }
}
