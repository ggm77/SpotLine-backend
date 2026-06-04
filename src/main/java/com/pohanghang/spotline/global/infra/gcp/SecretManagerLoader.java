package com.pohanghang.spotline.global.infra.gcp;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 기동 시 Secret Manager에서 시크릿을 읽어오는 데모성 로더.
 * enabled=false(기본)면 아무 것도 하지 않고, 실패해도 경고만 남기고 폴백한다
 * (기존 yaml @Value 주입은 그대로 유지 → 런타임 동작 변화 없음).
 */
@Component
public class SecretManagerLoader {

    private static final Logger log = LoggerFactory.getLogger(SecretManagerLoader.class);

    private final GcpProperties props;
    private final GcpRest rest;

    public SecretManagerLoader(final GcpProperties props, final GcpRest rest) {
        this.props = props;
        this.rest = rest;
    }

    @PostConstruct
    public void load() {
        if (!props.getSecretManager().isEnabled()) {
            return;
        }
        final String secretId = props.getSecretManager().getSecretId();
        try {
            final String url = "https://secretmanager.googleapis.com/v1/projects/" + props.getProjectId()
                    + "/secrets/" + secretId + "/versions/latest:access";
            final SecretResponse res = rest.getJson(url, SecretResponse.class);
            if (res != null && res.payload != null && res.payload.data != null) {
                final int bytes = Base64.getDecoder().decode(res.payload.data).length;
                log.info("Secret Manager 시크릿 '{}' 로드 성공 ({} bytes)", secretId, bytes);
            } else {
                log.warn("Secret Manager 응답이 비어 있음 — 기존 yaml 값으로 폴백: {}", secretId);
            }
        } catch (Exception e) {
            log.warn("Secret Manager 로드 실패 — 기존 yaml 값으로 폴백: {}", e.toString());
        }
    }

    public static class SecretResponse {
        public Payload payload;
    }

    public static class Payload {
        public String data;
    }
}
