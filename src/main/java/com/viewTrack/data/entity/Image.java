package com.viewTrack.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.UUID;

import static com.viewTrack.data.entity.AbstractEntity.DEFAULT_GENERATOR;

@Accessors(chain = true)
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "images")
@SequenceGenerator(name = DEFAULT_GENERATOR, sequenceName = "images_seq")
public class Image extends AbstractEntity {

    @Column(name = "upload_id", nullable = false)
    private UUID uploadId;

    @Column(name = "filename", nullable = false)
    private String filename;
}
