package com.pohanghang.spotline.global.infra.gcp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GCP 사이드채널 설정. 모든 제품 enabled 기본값은 false 라서, yaml에서 켜기 전까진
 * 어떤 GCP 호출도 일어나지 않는다(= 현재 동작과 완전히 동일).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gcp")
public class GcpProperties {

    /** Vertex AI와 동일한 GCP 프로젝트 ID */
    private String projectId = "knudc-amiproudchris";

    /** Cloud Tasks 등 리전 리소스 위치 */
    private String location = "asia-northeast3";

    /** Cloud Tasks 콜백이 호출할 백엔드 공개 베이스 URL */
    private String callbackBaseUrl = "https://spotline.seohamin.com";

    /** Cloud Tasks 콜백 보호용 공유 시크릿(콜백 헤더 검증). 비우면 검증 생략 */
    private String taskSecret = "";

    /** Cloud Tasks OIDC 토큰 발급용 서비스 계정 이메일. 비우면 OIDC 생략 */
    private String serviceAccountEmail = "";

    private final PubSub pubsub = new PubSub();
    private final Storage storage = new Storage();
    private final BigQuery bigquery = new BigQuery();
    private final Logging logging = new Logging();
    private final Monitoring monitoring = new Monitoring();
    private final SecretManager secretManager = new SecretManager();
    private final Tasks tasks = new Tasks();

    @Getter
    @Setter
    public static class PubSub {
        private boolean enabled = false;
        private String visionTopic = "spotline-vision-events";
        private String chunkTopic = "spotline-chunk-events";
    }

    @Getter
    @Setter
    public static class Storage {
        private boolean enabled = false;
        private String bucket = "spotline-archive";
    }

    @Getter
    @Setter
    public static class BigQuery {
        private boolean enabled = false;
        private String dataset = "spotline";
        private String table = "vision_events";
    }

    @Getter
    @Setter
    public static class Logging {
        private boolean enabled = false;
        private String logId = "spotline-sidechannel";
    }

    @Getter
    @Setter
    public static class Monitoring {
        private boolean enabled = false;
    }

    @Getter
    @Setter
    public static class SecretManager {
        private boolean enabled = false;
        private String secretId = "toss-pos-secret-key";
    }

    @Getter
    @Setter
    public static class Tasks {
        private boolean enabled = false;
        private String queue = "spotline-tasks";
    }
}
