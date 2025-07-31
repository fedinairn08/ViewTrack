package com.viewTrack.data.entity;

import com.viewTrack.data.enums.Type;
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
@Table(name = "user_movies")
@SequenceGenerator(name = DEFAULT_GENERATOR, sequenceName = "user_movies_seq")
public class UserMovie extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;
}
