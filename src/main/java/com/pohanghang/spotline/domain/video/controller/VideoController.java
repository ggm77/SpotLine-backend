package com.pohanghang.spotline.domain.video.controller;

import com.pohanghang.spotline.domain.video.VideoStreamSink;
import com.pohanghang.spotline.domain.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class VideoController {

    private final VideoService videoService;
    private final VideoStreamSink videoStreamSink;

    // Spring Boot → Frontend 실시간 스트리밍
    @GetMapping(value = "/video/stream", produces = "video/mp4")
    public ResponseEntity<Flux<DataBuffer>> streamToFrontend() {
        final Flux<DataBuffer> flux = videoStreamSink.flux()
                .map(bytes -> DefaultDataBufferFactory.sharedInstance.wrap(bytes));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(flux);
    }

    @PostMapping(value = "/video/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> streamVideo(
            @RequestParam(value = "createdAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAt,
            @RequestPart(value = "fileChunk") final MultipartFile fileChunk,
            @RequestParam(value = "sessionId", defaultValue = "default") final String sessionId
    ) {
        videoService.relayStreamChunk(createdAt, fileChunk, sessionId);
        return ResponseEntity.noContent().build();
    }
}
