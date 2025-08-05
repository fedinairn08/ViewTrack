package com.viewTrack.data.repository;

import com.viewTrack.data.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    @Query("SELECT m FROM Movie m LEFT JOIN FETCH m.genres g LEFT JOIN FETCH m.directors d")
    List<Movie> findAllWithGenresAndDirectors();

    @Query("""
    SELECT DISTINCT m FROM Movie m
    LEFT JOIN FETCH m.genres g
    LEFT JOIN FETCH m.directors d
    LEFT JOIN FETCH m.reviews r
    LEFT JOIN FETCH r.user u
    LEFT JOIN FETCH m.poster
    WHERE m.id = :id
    """)
    Optional<Movie> findByIdWithDetails(Long id);
}
