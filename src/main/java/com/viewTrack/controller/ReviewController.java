package com.viewTrack.controller;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.repository.MovieRepository;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.dto.RateMovieRequest;
import com.viewTrack.dto.request.SaveReviewRequest;
import com.viewTrack.dto.request.UpdateReviewRequest;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.ReviewService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    private final AuthUtils authUtils;

    private final MovieRepository movieRepository;

    private final ReviewRepository reviewRepository;

    @PostMapping("/rate-movie")
    public ResponseEntity<Map<String, Object>> rateMovie(@RequestBody RateMovieRequest request) {
        Review review = reviewService.rateMovie(request.getMovieId(), request.getRating());

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        Long ratingsCount = reviewRepository.countByMovie(movie);

        Map<String, Object> response = new HashMap<>();
        response.put("reviewId", review.getId());
        response.put("rating", review.getRating());
        response.put("content", review.getContent());
        response.put("averageRating", movie.getAverageRating());
        response.put("ratingsCount", ratingsCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-review")
    public ResponseEntity<Review> updateReview(@RequestBody SaveReviewRequest request) {
        User currentUser = authUtils.getUserEntity();
        Review updatedReview = reviewService.saveReview(
                currentUser.getId(),
                request.getMovieId(),
                request.getContent()
        );
        return ResponseEntity.ok(updatedReview);
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<Review> updateReview(@PathVariable Long reviewId, @RequestBody UpdateReviewRequest request) {
        User currentUser = authUtils.getUserEntity();
        Review updatedReview = reviewService.updateReview(
                currentUser.getId(),
                reviewId,
                request.getContent()
        );
        return ResponseEntity.ok(updatedReview);
    }

    @PutMapping("/reviews/delete/{reviewId}")
    public ResponseEntity<Review> deleteReview(@PathVariable Long reviewId) {
        User currentUser = authUtils.getUserEntity();
        Review updatedReview = reviewService.deleteReview(currentUser.getId(), reviewId);
        return ResponseEntity.ok(updatedReview);
    }
}
