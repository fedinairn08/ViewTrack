package com.viewTrack.data.repository;

import com.viewTrack.data.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByGenreName(String genreName);

    Boolean existsByGenreNameIgnoreCase(String genreName);

    Boolean existsByGenreName(String genreName);
}
