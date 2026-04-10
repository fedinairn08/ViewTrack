package com.viewTrack.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "kinopoisk")
public class KinopoiskProperties {

    private String apiKey;

    private String baseUrl = "https://api.poiskkino.dev";

    private int maxReviews = 5;
}
