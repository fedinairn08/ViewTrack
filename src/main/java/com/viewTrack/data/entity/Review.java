package com.viewTrack.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Table(name = "reviews")
@SequenceGenerator(name = DEFAULT_GENERATOR, sequenceName = "reviews_seq")
public class Review extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "rating", nullable = false)
    @Min(0)
    @Max(10)
    private int rating;
}
