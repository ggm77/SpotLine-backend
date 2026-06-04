package com.pohanghang.spotline.global.infra.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cloud Tasks 디스패치 콜백 수신점. 행위중립(로그만 남김)이며,
 * 공유 시크릿 헤더가 설정돼 있으면 검증한다.
 */
@RestController
@RequestMapping("/api/internal/gcp-tasks")
public class GcpTaskCallbackController {

    private static final Logger log = LoggerFactory.getLogger(GcpTaskCallbackController.class);

    private final GcpProperties props;

    public GcpTaskCallbackController(final GcpProperties props) {
        this.props = props;
    }

    @PostMapping("/vision")
    public ResponseEntity<Void> onVisionTask(
            @RequestHeader(value = "X-Task-Secret", required = false) final String secret,
            @RequestBody(required = false) final String body) {

        final String expected = props.getTaskSecret();
        if (!expected.isEmpty() && !expected.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Cloud Tasks 콜백 수신: {} bytes", body == null ? 0 : body.length());
        return ResponseEntity.ok().build();
    }
}
