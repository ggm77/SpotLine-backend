package com.pohanghang.spotline.domain.vision.controller;

import com.pohanghang.spotline.domain.video.VideoStreamSink;
import com.pohanghang.spotline.domain.vision.dto.VisionDataRequestDto;
import com.pohanghang.spotline.domain.vision.service.VisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class VisionController {

    private final VisionService visionService;
    private final VideoStreamSink videoStreamSink;

    // 비전 AI 분석 데이터 입력
    @PostMapping("/vision/data")
    public ResponseEntity<Void> createVisionData(
            @RequestBody final VisionDataRequestDto visionDataRequestDto
    ) {
        visionService.saveVisionData(visionDataRequestDto);
        return ResponseEntity.noContent().build();
    }

    // FastAPI → Spring Boot 영상 청크 수신
    @PostMapping(value = "/vision/stream", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> receiveVideoChunk(@RequestBody final byte[] chunk) {
        videoStreamSink.emit(chunk);
        return ResponseEntity.noContent().build();
    }
}
