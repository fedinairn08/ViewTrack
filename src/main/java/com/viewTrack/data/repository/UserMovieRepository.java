package com.viewTrack.data.repository;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.entity.UserMovie;
import com.viewTrack.data.enums.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMovieRepository extends JpaRepository<UserMovie, Long> {
    List<UserMovie> findByUserAndType(User user, Type type);

    Optional<UserMovie> findByUserAndMovie(User user, Movie movie);

    List<UserMovie> findByUser(User user);

    Long countByUserIdAndType(Long userId, Type type);

    List<UserMovie> findByMovie(Movie movie);
}
