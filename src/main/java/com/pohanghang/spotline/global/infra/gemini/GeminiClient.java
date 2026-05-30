package com.pohanghang.spotline.global.infra.gemini;

import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.gemini.dto.GeminiRequestDto;
import com.pohanghang.spotline.global.infra.gemini.dto.GeminiResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class GeminiClient {

    private final WebClient geminiWebClient;

    @Value("${gemini.key}")
    private String geminiKey;

    public GeminiClient(@Qualifier("geminiWebClient") final WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
    }

    public String generateContent(final String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        GeminiRequestDto requestDto = GeminiRequestDto.of(prompt);

        GeminiResponseDto response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(":generateContent")
                        .queryParam("key", geminiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(GeminiResponseDto.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .block();

        if (response == null) {
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        return response.getFirstText();
    }
}
