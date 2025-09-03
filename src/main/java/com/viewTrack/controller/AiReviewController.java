package com.viewTrack.controller;

import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.service.AiReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ai-reviews")
@RequiredArgsConstructor
public class AiReviewController {

    private final AiReviewService aiReviewService;

    @PostMapping("/regenerate/{movieId}")
    public ResponseEntity<BasicApiResponse<String>> regenerateReview(@PathVariable Long movieId) {
        try {
            aiReviewService.regenerateReviewForMovie(movieId);
            
            return ResponseEntity.ok(new BasicApiResponse<>("Рецензия успешно перегенерирована"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new BasicApiResponse<>(true, "Ошибка при перегенерации рецензии"));
        }
    }
}
