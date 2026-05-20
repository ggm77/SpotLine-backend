package com.pohanghang.spotline.global.infra.video;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class YoloClient {

    @Value("${yolo.analyze-uri}")
    private String ANALYZE_URI;

    private final WebClient yoloWebClient;

    public String analyzeVideo(final String fileName) {
        return yoloWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(ANALYZE_URI)
                        .queryParam("path", fileName)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
