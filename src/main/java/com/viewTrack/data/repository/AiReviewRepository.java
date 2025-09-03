package com.viewTrack.data.repository;

import com.viewTrack.data.entity.AiReview;
import com.viewTrack.data.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiReviewRepository extends JpaRepository<AiReview, Long> {
    
    Optional<AiReview> findByMovie(Movie movie);
}

