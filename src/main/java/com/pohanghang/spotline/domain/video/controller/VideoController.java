package com.pohanghang.spotline.domain.video.controller;

import com.pohanghang.spotline.domain.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class VideoController {

    private final VideoService videoService;

    @PostMapping(value = "/video/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> streamVideo(
            @RequestParam(value = "createdAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAt,
            @RequestPart(value = "fileChunk") final MultipartFile fileChunk
    ) {

        videoService.saveStreamChunk(createdAt, fileChunk);

        return ResponseEntity.noContent().build();
    }
}
