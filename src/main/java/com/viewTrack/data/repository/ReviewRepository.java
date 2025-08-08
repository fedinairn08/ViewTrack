package com.viewTrack.data.repository;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByUserAndMovie(User user, Movie movie);

    Long countByUser_Id(Long userId);

    List<Review> findByMovieId(Long movieId);

    Long countByMovie(Movie movie);

    List<Review> findByMovie(Movie movie);
}
