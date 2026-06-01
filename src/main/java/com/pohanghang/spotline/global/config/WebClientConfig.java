package com.pohanghang.spotline.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${gemini.url}")
    private String GEMINI_URL;

    @Value("${toss-pos.base-url}")
    private String TOSS_POS_BASE_URL;

    @Value("${relay.base-url}")
    private String RELAY_BASE_URL;

    @Bean
    public WebClient openMeteoWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.open-meteo.com")
                .build();
    }

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(GEMINI_URL)
                .build();
    }

    @Bean
    public WebClient tossPosWebClient() {
        return WebClient.builder()
                .baseUrl(TOSS_POS_BASE_URL)
                .build();
    }

    @Bean
    public WebClient relayWebClient() {
        return WebClient.builder()
                .baseUrl(RELAY_BASE_URL)
                .build();
    }
}
