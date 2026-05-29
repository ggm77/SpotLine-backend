package com.pohanghang.spotline.domain.vision.controller;

import com.pohanghang.spotline.domain.vision.dto.VisionDataRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class VisionController {

    // 비전 데이터 받는 API
    @PostMapping("/vision/data")
    public ResponseEntity<Void> createVisionData(
            @RequestBody final VisionDataRequestDto visionDataRequestDto
    ) {

        // mock
        return ResponseEntity.notFound().build();
    }
}
