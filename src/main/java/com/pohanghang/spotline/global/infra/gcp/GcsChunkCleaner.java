package com.pohanghang.spotline.global.infra.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * GCS의 chunks/ 객체를 1시간마다 정리한다(보존 시간 초과분 삭제).
 * storage.enabled가 켜져 청크가 실제로 업로드될 때만 동작하며,
 * 실패해도 경고만 남기고 다음 주기에 재시도한다.
 */
@Component
public class GcsChunkCleaner {

    private static final Logger log = LoggerFactory.getLogger(GcsChunkCleaner.class);
    private static final String PREFIX = "chunks/";

    private final GcpProperties props;
    private final GcpRest rest;

    public GcsChunkCleaner(final GcpProperties props, final GcpRest rest) {
        this.props = props;
        this.rest = rest;
    }

    // 매시 정각 실행
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void cleanup() {
        if (!props.getStorage().isEnabled()) {
            return;
        }
        final String bucket = props.getStorage().getBucket();
        final int retentionMinutes = props.getStorage().getChunkRetentionMinutes();
        final Instant cutoff = Instant.now().minus(Duration.ofMinutes(retentionMinutes));

        try {
            int deleted = 0;
            String pageToken = null;
            do {
                final ListResponse res = list(bucket, pageToken);
                if (res == null) {
                    break;
                }
                if (res.items != null) {
                    for (final Item item : res.items) {
                        if (item.name == null || item.timeCreated == null) {
                            continue;
                        }
                        if (Instant.parse(item.timeCreated).isBefore(cutoff)) {
                            deleteObject(bucket, item.name);
                            deleted++;
                        }
                    }
                }
                pageToken = res.nextPageToken;
            } while (pageToken != null);

            log.info("GCS 청크 정리 완료: {}개 삭제 (보존 {}분, 버킷 {})", deleted, retentionMinutes, bucket);
        } catch (Exception e) {
            log.warn("GCS 청크 정리 실패(무시, 다음 주기 재시도): {}", e.toString());
        }
    }

    private ListResponse list(final String bucket, final String pageToken) {
        String url = "https://storage.googleapis.com/storage/v1/b/" + bucket
                + "/o?prefix=" + URLEncoder.encode(PREFIX, StandardCharsets.UTF_8)
                + "&fields=" + URLEncoder.encode("items(name,timeCreated),nextPageToken", StandardCharsets.UTF_8);
        if (pageToken != null) {
            url += "&pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8);
        }
        return rest.getJson(url, ListResponse.class);
    }

    private void deleteObject(final String bucket, final String objectName) {
        final String url = "https://storage.googleapis.com/storage/v1/b/" + bucket
                + "/o/" + URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        rest.delete(url);
    }

    public static class ListResponse {
        public List<Item> items;
        public String nextPageToken;
    }

    public static class Item {
        public String name;
        public String timeCreated;
    }
}
