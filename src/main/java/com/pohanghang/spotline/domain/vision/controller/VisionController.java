package com.pohanghang.spotline.domain.vision.controller;

import com.pohanghang.spotline.domain.video.VideoStreamSink;
import com.pohanghang.spotline.domain.vision.dto.VisionDataRequestDto;
import com.pohanghang.spotline.domain.vision.dto.VisionDataResponseDto;
import com.pohanghang.spotline.domain.vision.service.VisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestPart("data") final VisionDataRequestDto visionDataRequestDto,
            @RequestPart(value = "video", required = false) final MultipartFile video
    ) throws IOException {
        visionService.saveVisionData(visionDataRequestDto);
        if (video != null && !video.isEmpty()) {
            videoStreamSink.emit(video.getBytes());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vision/data/latest")
    public ResponseEntity<VisionDataResponseDto> getLatestVisionData() {
        return ResponseEntity.ok(visionService.getLatestVisionData());
    }
}
