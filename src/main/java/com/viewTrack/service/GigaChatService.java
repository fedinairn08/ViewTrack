package com.viewTrack.service;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;

import java.util.List;

public interface GigaChatService {

    String generateMovieReview(Movie movie, List<Review> reviews);
}
