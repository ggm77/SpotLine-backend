package com.pohanghang.spotline.domain.video.controller;

import com.pohanghang.spotline.domain.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/video/stream")
    public Mono<ResponseEntity<Flux<DataBuffer>>> proxyStream() {
        return videoService.proxyStream();
    }

    @PostMapping(value = "/video/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> streamVideo(
            @RequestParam(value = "createdAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAt,
            @RequestPart(value = "fileChunk") final MultipartFile fileChunk
    ) {

        videoService.relayStreamChunk(createdAt, fileChunk);

        return ResponseEntity.noContent().build();
    }
}
