package com.viewTrack.controller;

import com.viewTrack.data.entity.Review;
import com.viewTrack.dto.RateMovieRequest;
import com.viewTrack.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/user/rate-movie")
    public ResponseEntity<Review> rateMovie(@RequestBody RateMovieRequest request) {
        Review review = reviewService.rateMovie(request.getMovieId(), request.getRating());
        return ResponseEntity.ok(review);
    }
}
