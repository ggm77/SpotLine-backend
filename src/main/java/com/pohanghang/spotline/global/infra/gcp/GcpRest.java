package com.pohanghang.spotline.global.infra.gcp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;

/**
 * GCP REST API 공통 호출기. GeminiClient와 같은 ADC Bearer 토큰 방식.
 * 사이드채널 전용 풀에서 .block()으로 호출되므로 동기 스타일을 그대로 쓴다.
 */
@Component
public class GcpRest {

    private final WebClient gcpWebClient;
    private final GcpAccessTokenProvider tokenProvider;

    public GcpRest(@Qualifier("gcpWebClient") final WebClient gcpWebClient,
                   final GcpAccessTokenProvider tokenProvider) {
        this.gcpWebClient = gcpWebClient;
        this.tokenProvider = tokenProvider;
    }

    public void postJson(final String url, final Object body) {
        gcpWebClient.post()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    public void postBytes(final String url, final byte[] data, final MediaType contentType) {
        gcpWebClient.post()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(contentType)
                .bodyValue(data)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(15))
                .block();
    }

    public <T> T getJson(final String url, final Class<T> type) {
        return gcpWebClient.get()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .bodyToMono(type)
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    public void delete(final String url) {
        gcpWebClient.delete()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    private String bearer() {
        return "Bearer " + tokenProvider.getToken();
    }
}
