package com.viewTrack.config;

import chat.giga.client.GigaChatClient;
import chat.giga.client.GigaChatClientImpl;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GigaChatConfiguration {

    @Value("${gigachat.secret}")
    private String clientSecret;

    @Bean
    public GigaChatClient gigaChatClient() {
        return GigaChatClientImpl.builder()
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS)
                                .authKey(clientSecret)
                                .build())
                        .build())
                .build();
    }
}
