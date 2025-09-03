package com.viewTrack.data.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import static com.viewTrack.data.entity.AbstractEntity.DEFAULT_GENERATOR;

@Accessors(chain = true)
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ai_reviews")
@SequenceGenerator(name = DEFAULT_GENERATOR, sequenceName = "ai_reviews_seq")
public class AiReview extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "reviews_count_at_generation", nullable = false)
    private Integer reviewsCountAtGeneration;
}

