package com.pohanghang.spotline.global.infra.gemini;

import com.google.auth.oauth2.GoogleCredentials;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.gemini.dto.GeminiRequestDto;
import com.pohanghang.spotline.global.infra.gemini.dto.GeminiResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final WebClient geminiWebClient;

    @Value("${vertex-ai.project-id}")
    private String projectId;

    @Value("${vertex-ai.location}")
    private String location;

    @Value("${vertex-ai.model}")
    private String model;

    public GeminiClient(@Qualifier("geminiWebClient") final WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
    }

    public String generateContent(final String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        return postGenerateContent(GeminiRequestDto.of(prompt));
    }

    public String chat(final List<GeminiRequestDto.Content> contents, final String systemInstruction) {
        if (contents == null || contents.isEmpty()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        return postGenerateContent(GeminiRequestDto.ofChat(contents, systemInstruction));
    }

    private String postGenerateContent(final GeminiRequestDto request) {
        final String path = String.format(
                "/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                projectId, location, model
        );

        try {
            GeminiResponseDto response = geminiWebClient.post()
                    .uri(path)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponseDto.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(e -> !(e instanceof WebClientResponseException wcre && wcre.getStatusCode().is4xxClientError())))
                    .block();

            if (response == null) {
                throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
            }

            return response.getFirstText();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof WebClientResponseException wcre) {
                log.error("Vertex AI 오류 {} - 응답 바디: {}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
            } else {
                log.error("Vertex AI 호출 실패: {}", e.getMessage());
            }
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String getAccessToken() {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new RuntimeException("Vertex AI 인증 토큰 획득 실패", e);
        }
    }
}
