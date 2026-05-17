package com.pohanghang.spotline.domain.video.controller;

import com.pohanghang.spotline.domain.video.dto.*;
import com.pohanghang.spotline.domain.video.entity.Status;
import com.pohanghang.spotline.domain.video.entity.Weather;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VideoController {

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoUploadResponseDto> uploadVideo(
            @RequestPart(value = "file") final MultipartFile multipartFile,
            @RequestParam(value = "startAt") final LocalDateTime startAt,
            @RequestParam(value = "endAt") final LocalDateTime endAt,
            @RequestParam(value = "weather") final String weatherStr,
            @RequestParam(value = "detail") final String detail
    ) {

        // mock
        return ResponseEntity.ok().body(new VideoUploadResponseDto(1L));
    }

    @GetMapping("/video")
    public ResponseEntity<VideoInfoListResponseDto> getAllVideoInfo() {

        // mock
        return ResponseEntity.ok().body(new VideoInfoListResponseDto(
                Arrays.asList(
                    new VideoInfoResponseDto(
                        1L,
                        10,
                        50,
                        10,
                        5,
                        5,
                        1,
                        2,
                        Arrays.asList(0, 10, 0, 0, 0, 0, 0, 0, 0),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        Weather.SUNNY
                    )
                )
        ));
    }

    @GetMapping("/video/{id}/status")
    public ResponseEntity<VideoStatusResponseDto> getStatus(
            @PathVariable final Long id
    ) {

        // mock
        return ResponseEntity.ok().body(new VideoStatusResponseDto(Status.COMPLETE));
    }

    @GetMapping("/video/{id}")
    public ResponseEntity<VideoInfoResponseDto> getVideoInfo(
            @PathVariable final Long id
    ) {

        //mock
        return ResponseEntity.ok().body(new VideoInfoResponseDto(
                1L,
                10,
                50,
                10,
                5,
                5,
                1,
                2,
                Arrays.asList(0, 10, 0, 0, 0, 0, 0, 0, 0),
                LocalDateTime.now(),
                LocalDateTime.now(),
                Weather.SUNNY
        ));
    }

    @GetMapping("/video/{id}/feedback")
    public ResponseEntity<VideoFeedbackResponseDto> getFeedback(
            @PathVariable final Long id
    ) {

        // mock
        return ResponseEntity.ok().body(new VideoFeedbackResponseDto("mock테스트 - 분석 결과", "mock테스트 - 피드백"));
    }

    @DeleteMapping("/video/{id}")
    public ResponseEntity<Void> deleteVideo(
            @PathVariable final Long id
    ) {

        // mock
        return ResponseEntity.noContent().build();
    }
}
