package com.pohanghang.spotline.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${yolo.url}")
    private String YOLO_URL;

    @Value("${gemini.url}")
    private String GEMINI_URL;

    @Bean
    public WebClient yoloWebClient() {
        return WebClient.builder()
                .baseUrl(YOLO_URL)
                .build();
    }

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(GEMINI_URL)
                .build();
    }
}
