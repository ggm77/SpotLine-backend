package com.pohanghang.spotline.domain.video.controller;

import com.pohanghang.spotline.domain.video.service.VideoService;
import com.pohanghang.spotline.domain.video.dto.VideoStatusResponseDto;
import com.pohanghang.spotline.domain.video.dto.VideoUploadResponseDto;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class VideoController {

    private final VideoService videoService;

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoUploadResponseDto> uploadVideo(
            @RequestPart(value = "file") final MultipartFile multipartFile,
            @RequestParam(value = "startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam(value = "endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {

        return ResponseEntity.ok(videoService.uploadVideo(multipartFile, startAt, endAt));
    }

    @PostMapping(value = "/video/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> streamVideo(
            @RequestParam(value = "createdAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAt,
            @RequestPart(value = "fileChunk") final MultipartFile fileChunk
    ) {

        videoService.saveStreamChunk(createdAt, fileChunk);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/video/{id}")
    public ResponseEntity<Resource> downloadVideo(
            @PathVariable final Long id
    ) {

        final Resource resource = videoService.downloadVideo(id);

        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IOException e) {
            throw new CustomException(ExceptionCode.VIDEO_NOT_FOUND);
        }
    }

    @GetMapping("/video/{id}/status")
    public ResponseEntity<VideoStatusResponseDto> getStatus(
            @PathVariable final Long id
    ) {

        return ResponseEntity.ok(videoService.getStatus(id));
    }
}
