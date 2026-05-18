package com.pohanghang.spotline.domain.video.controller;

import com.pohanghang.spotline.domain.video.controller.service.VideoService;
import com.pohanghang.spotline.domain.video.dto.VideoStatusResponseDto;
import com.pohanghang.spotline.domain.video.dto.VideoUploadResponseDto;
import com.pohanghang.spotline.domain.video.entity.Status;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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
            @RequestParam(value = "startAt") final LocalDateTime startAt,
            @RequestParam(value = "endAt") final LocalDateTime endAt
    ) {

        return ResponseEntity.ok(videoService.uploadVideo(multipartFile, startAt, endAt));
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
