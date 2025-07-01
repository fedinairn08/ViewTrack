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
@Table(name = "streaming_services")
@SequenceGenerator(name = DEFAULT_GENERATOR, sequenceName = "streaming_services_seq")
public class StreamingService extends AbstractEntity {

    @Column(name = "platform_name", nullable = false)
    private String platformName;

    @Column(name = "price_per_month")
    private double pricePerMonth;
}
