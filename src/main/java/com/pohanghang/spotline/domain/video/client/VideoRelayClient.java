package com.pohanghang.spotline.domain.video.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class VideoRelayClient {

    private final WebClient relayWebClient;

    @Value("${relay.stream-path}")
    private String streamPath;

    public VideoRelayClient(@Qualifier("relayWebClient") final WebClient relayWebClient) {
        this.relayWebClient = relayWebClient;
    }

    public void relayChunk(final LocalDateTime createdAt, final MultipartFile fileChunk) {
        try {
            final byte[] bytes = fileChunk.getBytes();
            final String originalFilename = fileChunk.getOriginalFilename();

            final MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("fileChunk", new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return originalFilename != null ? originalFilename : "chunk.mp4";
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);
            builder.part("createdAt", createdAt.toString());

            relayWebClient.post()
                    .uri(streamPath)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (IOException e) {
            throw new RuntimeException("영상 청크 중계 중 오류 발생", e);
        }
    }
}
