package com.pohanghang.spotline.domain.vision.controller;

import com.pohanghang.spotline.domain.video.VideoStreamSink;
import com.pohanghang.spotline.domain.vision.dto.VisionDataRequestDto;
import com.pohanghang.spotline.domain.vision.service.VisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class VisionController {

    private final VisionService visionService;
    private final VideoStreamSink videoStreamSink;

    @PostMapping(value = "/vision/data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createVisionData(
            @RequestPart("data") final VisionDataRequestDto visionDataRequestDto
    ) {
        visionService.saveVisionData(visionDataRequestDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/vision/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> receiveVideoChunk(
            @RequestPart("video") final MultipartFile video
    ) throws IOException {
        videoStreamSink.emit(video.getBytes());
        return ResponseEntity.noContent().build();
    }
}
