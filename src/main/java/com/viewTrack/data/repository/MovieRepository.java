package com.viewTrack.data.repository;

import com.viewTrack.data.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres")
    List<Movie> findAllWithGenres();
}
