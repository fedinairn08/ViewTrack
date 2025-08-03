package com.viewTrack.controller;

import com.viewTrack.data.entity.Review;
import com.viewTrack.data.entity.User;
import com.viewTrack.dto.RateMovieRequest;
import com.viewTrack.dto.request.UpdateReviewRequest;
import com.viewTrack.service.ReviewService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    private final AuthUtils authUtils;

    @PostMapping("/rate-movie")
    public ResponseEntity<Review> rateMovie(@RequestBody RateMovieRequest request) {
        Review review = reviewService.rateMovie(request.getMovieId(), request.getRating());
        return ResponseEntity.ok(review);
    }

    @PostMapping("/update-review")
    public ResponseEntity<Review> updateReview(@RequestBody UpdateReviewRequest request) {
        User currentUser = authUtils.getUserEntity();
        Review updatedReview = reviewService.updateReview(
                currentUser.getId(),
                request.getMovieId(),
                request.getContent()
        );
        return ResponseEntity.ok(updatedReview);
    }
}
