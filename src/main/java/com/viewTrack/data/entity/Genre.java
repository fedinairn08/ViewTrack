package com.viewTrack.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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
@Table(name = "genres")
@SequenceGenerator(name = DEFAULT_GENERATOR, sequenceName = "genres_seq")
public class Genre extends AbstractEntity {

    @Column(name = "genre_name", nullable = false)
    private String genreName;
}
