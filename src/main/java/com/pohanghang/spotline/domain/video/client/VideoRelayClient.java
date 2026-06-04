package com.pohanghang.spotline.domain.video.client;

import com.pohanghang.spotline.global.infra.gcp.GcpSideChannel;
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
import java.time.format.DateTimeFormatter;


@Component
public class VideoRelayClient {

    private final WebClient relayWebClient;
    private final GcpSideChannel gcpSideChannel;

    @Value("${relay.stream-path}")
    private String streamPath;

    public VideoRelayClient(@Qualifier("relayWebClient") final WebClient relayWebClient,
                            final GcpSideChannel gcpSideChannel) {
        this.relayWebClient = relayWebClient;
        this.gcpSideChannel = gcpSideChannel;
    }

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public void relayChunk(final LocalDateTime createdAt, final MultipartFile fileChunk, final String sessionId) {
        try {
            final byte[] bytes = fileChunk.getBytes();
            final String originalFilename = fileChunk.getOriginalFilename();

            final MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return originalFilename != null ? originalFilename : "chunk.mp4";
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);
            builder.part("createdAt", createdAt.format(ISO_UTC));
            builder.part("sessionId", sessionId);

            relayWebClient.post()
                    .uri(streamPath)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            gcpSideChannel.onChunkRelayed(createdAt, bytes, sessionId);

        } catch (IOException e) {
            throw new RuntimeException("영상 청크 중계 중 오류 발생", e);
        }
    }
}
