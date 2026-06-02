package com.pohanghang.spotline.domain.video.service;

import com.pohanghang.spotline.domain.video.client.VideoRelayClient;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRelayClient videoRelayClient;

    public Mono<ResponseEntity<Flux<DataBuffer>>> proxyStream() {
        return videoRelayClient.fetchStream();
    }

    public void relayStreamChunk(final LocalDateTime createdAt, final MultipartFile fileChunk) {
        if (createdAt == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }
        if (fileChunk == null || fileChunk.isEmpty()) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        videoRelayClient.relayChunk(createdAt, fileChunk);
    }
}
