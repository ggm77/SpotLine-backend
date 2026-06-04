package com.pohanghang.spotline.domain.video.controller;

import com.pohanghang.spotline.domain.video.VideoStreamSink;
import com.pohanghang.spotline.domain.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.stream.Stream;


@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class VideoController {

    private final VideoService videoService;
    private final VideoStreamSink videoStreamSink;

    // Spring Boot → Frontend 실시간 스트리밍
    @GetMapping(value = "/video/stream", produces = "video/mp4")
    public ResponseEntity<StreamingResponseBody> streamToFrontend() {
        // 서블릿 네이티브 스트리밍: Flux<byte[]> 를 OutputStream 으로 브리지하며 청크마다 flush
        final StreamingResponseBody body = outputStream -> {
            try (Stream<byte[]> stream = videoStreamSink.flux().toStream()) {
                final Iterator<byte[]> iterator = stream.iterator();
                while (iterator.hasNext()) {
                    final byte[] chunk = iterator.next();
                    try {
                        outputStream.write(chunk);
                        outputStream.flush();
                    } catch (IOException e) {
                        // 클라이언트 연결 종료 → 구독 정리 후 종료
                        break;
                    }
                }
            }
        };
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(body);
    }

    @PostMapping(value = "/video/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> streamVideo(
            @RequestParam(value = "createdAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAt,
            @RequestPart(value = "fileChunk") final MultipartFile fileChunk,
            @RequestParam(value = "sessionId", defaultValue = "default") final String sessionId
    ) {
        videoService.relayStreamChunk(createdAt, fileChunk, sessionId);
        return ResponseEntity.noContent().build();
    }
}
