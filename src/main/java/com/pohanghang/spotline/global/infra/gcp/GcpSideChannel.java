package com.pohanghang.spotline.global.infra.gcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pohanghang.spotline.domain.vision.dto.VisionDataRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 기존 데이터 흐름을 탭해서 여러 GCP 제품으로 복사 전송하는 사이드채널.
 * 전부 전용 풀에서 fire-and-forget로 돌고 예외를 삼키므로, 원래 요청 경로
 * (relay, DB 저장, sink emit, 프론트 스트림)에는 어떤 영향도 주지 않는다.
 * 각 제품은 자신의 enabled 플래그가 켜졌을 때만 실제 호출한다.
 */
@Component
public class GcpSideChannel {

    private static final Logger log = LoggerFactory.getLogger(GcpSideChannel.class);

    private final GcpProperties props;
    private final GcpRest rest;
    // Spring Boot 4는 Jackson 3(tools.jackson)을 빈으로 노출 → com.fasterxml ObjectMapper 빈이 없으므로 직접 생성
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Executor executor;

    public GcpSideChannel(final GcpProperties props,
                          final GcpRest rest,
                          @Qualifier("gcpSideChannelExecutor") final Executor executor) {
        this.props = props;
        this.rest = rest;
        this.executor = executor;
    }

    /** 분석 데이터 수신(POST /api/v2/vision/data) 직후 호출 */
    public void onVisionData(final VisionDataRequestDto dto, final byte[] videoBytes) {
        submit("pubsub", () -> publishVisionEvent(dto));
        submit("storage", () -> archiveFrame(videoBytes));
        submit("bigquery", () -> insertVisionRow(dto));
        submit("logging", () -> writeVisionLog(dto));
        submit("monitoring", () -> recordGauge("vision_data_count", 1));
        submit("tasks", () -> enqueueVisionTask(dto));
    }

    /** 원본 청크 중계(POST /api/v1/video/stream) 직후 호출 */
    public void onChunkRelayed(final LocalDateTime createdAt, final byte[] chunk, final String sessionId) {
        final int size = chunk == null ? 0 : chunk.length;
        submit("storage", () -> archiveChunk(chunk));
        submit("pubsub", () -> publishChunkEvent(sessionId, createdAt, size));
        submit("monitoring", () -> recordGauge("chunk_relay_count", size));
    }

    private void submit(final String name, final Runnable task) {
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    log.warn("GCP 사이드채널[{}] 실패(무시): {}", name, e.toString());
                }
            });
        } catch (Exception ignored) {
            // DiscardPolicy라 보통 도달하지 않음. 만약을 위해 무시
        }
    }

    // ---- Pub/Sub ----
    private void publishVisionEvent(final VisionDataRequestDto dto) {
        if (!props.getPubsub().isEnabled()) return;
        final String url = "https://pubsub.googleapis.com/v1/projects/" + props.getProjectId()
                + "/topics/" + props.getPubsub().getVisionTopic() + ":publish";
        final Map<String, Object> message = Map.of(
                "data", base64(toJson(dto)),
                "attributes", Map.of("type", "vision-data", "source", "spotline-backend"));
        rest.postJson(url, Map.of("messages", List.of(message)));
    }

    private void publishChunkEvent(final String sessionId, final LocalDateTime createdAt, final int sizeBytes) {
        if (!props.getPubsub().isEnabled()) return;
        final String url = "https://pubsub.googleapis.com/v1/projects/" + props.getProjectId()
                + "/topics/" + props.getPubsub().getChunkTopic() + ":publish";
        final Map<String, Object> payload = Map.of(
                "sessionId", sessionId == null ? "unknown" : sessionId,
                "createdAt", createdAt == null ? "" : createdAt.toString(),
                "sizeBytes", sizeBytes);
        rest.postJson(url, Map.of("messages", List.of(Map.of("data", base64(toJson(payload))))));
    }

    // ---- Cloud Storage ----
    private void archiveFrame(final byte[] videoBytes) {
        if (!props.getStorage().isEnabled() || videoBytes == null || videoBytes.length == 0) return;
        rest.postBytes(
                storageUploadUrl("vision-frames/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".bin"),
                videoBytes, MediaType.APPLICATION_OCTET_STREAM);
    }

    private void archiveChunk(final byte[] chunk) {
        if (!props.getStorage().isEnabled() || chunk == null || chunk.length == 0) return;
        rest.postBytes(
                storageUploadUrl("chunks/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".mp4"),
                chunk, MediaType.APPLICATION_OCTET_STREAM);
    }

    private String storageUploadUrl(final String objectName) {
        return "https://storage.googleapis.com/upload/storage/v1/b/" + props.getStorage().getBucket()
                + "/o?uploadType=media&name=" + URLEncoder.encode(objectName, StandardCharsets.UTF_8);
    }

    // ---- BigQuery ----
    private void insertVisionRow(final VisionDataRequestDto dto) {
        if (!props.getBigquery().isEnabled()) return;
        final String url = "https://bigquery.googleapis.com/bigquery/v2/projects/" + props.getProjectId()
                + "/datasets/" + props.getBigquery().getDataset()
                + "/tables/" + props.getBigquery().getTable() + "/insertAll";
        final Map<String, Object> row = Map.of(
                "ingested_at", Instant.now().toString(),
                "payload", toJson(dto));
        rest.postJson(url, Map.of("rows", List.of(Map.of("json", row))));
    }

    // ---- Cloud Logging ----
    private void writeVisionLog(final VisionDataRequestDto dto) {
        if (!props.getLogging().isEnabled()) return;
        final String url = "https://logging.googleapis.com/v2/entries:write";
        final Map<String, Object> entry = Map.of(
                "severity", "INFO",
                "jsonPayload", Map.of("event", "vision-data", "payload", toMap(dto)));
        rest.postJson(url, Map.of(
                "logName", "projects/" + props.getProjectId() + "/logs/" + props.getLogging().getLogId(),
                "resource", Map.of("type", "global"),
                "entries", List.of(entry)));
    }

    // ---- Cloud Monitoring ----
    private void recordGauge(final String metric, final long value) {
        if (!props.getMonitoring().isEnabled()) return;
        final String url = "https://monitoring.googleapis.com/v3/projects/" + props.getProjectId() + "/timeSeries";
        final Map<String, Object> timeSeries = Map.of(
                "metric", Map.of("type", "custom.googleapis.com/spotline/" + metric),
                "resource", Map.of("type", "global", "labels", Map.of("project_id", props.getProjectId())),
                "points", List.of(Map.of(
                        "interval", Map.of("endTime", Instant.now().toString()),
                        "value", Map.of("int64Value", String.valueOf(value)))));
        rest.postJson(url, Map.of("timeSeries", List.of(timeSeries)));
    }

    // ---- Cloud Tasks ----
    private void enqueueVisionTask(final VisionDataRequestDto dto) {
        if (!props.getTasks().isEnabled()) return;
        final String url = "https://cloudtasks.googleapis.com/v2/projects/" + props.getProjectId()
                + "/locations/" + props.getLocation()
                + "/queues/" + props.getTasks().getQueue() + "/tasks";

        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (!props.getTaskSecret().isEmpty()) {
            headers.put("X-Task-Secret", props.getTaskSecret());
        }

        final Map<String, Object> httpRequest = new LinkedHashMap<>();
        httpRequest.put("url", props.getCallbackBaseUrl() + "/api/internal/gcp-tasks/vision");
        httpRequest.put("httpMethod", "POST");
        httpRequest.put("headers", headers);
        httpRequest.put("body", base64(toJson(dto)));
        if (!props.getServiceAccountEmail().isEmpty()) {
            httpRequest.put("oidcToken", Map.of("serviceAccountEmail", props.getServiceAccountEmail()));
        }

        rest.postJson(url, Map.of("task", Map.of("httpRequest", httpRequest)));
    }

    // ---- helpers ----
    private String toJson(final Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(final Object o) {
        return objectMapper.convertValue(o, Map.class);
    }

    private String base64(final String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
