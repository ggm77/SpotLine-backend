package com.pohanghang.spotline.domain.video.controller;

import com.pohanghang.spotline.domain.video.dto.VideoStatusResponseDto;
import com.pohanghang.spotline.domain.video.dto.VideoUploadResponseDto;
import com.pohanghang.spotline.domain.analytics.entity.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class VideoController {

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoUploadResponseDto> uploadVideo(
            @RequestPart(value = "file") final MultipartFile multipartFile,
            @RequestParam(value = "startAt") final LocalDateTime startAt,
            @RequestParam(value = "endAt") final LocalDateTime endAt
    ) {

        //mock
        return ResponseEntity.ok(new VideoUploadResponseDto(1L));
    }

    @GetMapping("/video/{id}")
    public ResponseEntity<Resource> downloadVideo(
            @PathVariable final Long id
    ) {

        //mock
        byte[] mockData = "mock video binary data".getBytes();
        Resource resource = new ByteArrayResource(mockData);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"video.mp4\"")
                .contentLength(mockData.length)
                .body(resource);
    }

    @GetMapping("/video/{id}/status")
    public ResponseEntity<VideoStatusResponseDto> getStatus(
            @PathVariable final Long id
    ) {

        //mock
        return ResponseEntity.ok(new VideoStatusResponseDto(Status.COMPLETE));
    }
}
