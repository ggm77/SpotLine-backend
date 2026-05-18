package com.pohanghang.spotline.domain.analytics.entity;

import com.pohanghang.spotline.domain.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "analytics", indexes = {
        @Index(name = "idx_analytics_duration", columnList = "startAt, endAt")
})
public class Analytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @Column(nullable = false)
    private LocalDateTime startAt; // 영상 시작 시간

    @Column(nullable = false)
    private LocalDateTime endAt; // 영상 종료 시간

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Congestion peakCongestion; // 피크 혼잡도

    @Column(nullable = false)
    private Double avgDwellTimeSeconds; // 평균 체류시간(초)

    @Column(nullable = false)
    private Integer totalCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Weather weather;

    @Column(nullable = false)
    private Double temperature;

    @Lob
    @Column(nullable = false)
    private String rawData; // 영상에서 추출한 원본 데이터 그대로

    @OneToMany(mappedBy = "analytics", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalyticsPerson> persons;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Analytics(
            final Video video,
            final LocalDateTime startAt,
            final LocalDateTime endAt,
            final Congestion peakCongestion,
            final Double avgDwellTimeSeconds,
            final Integer totalCount,
            final Weather weather,
            final Double temperature,
            final String rawData
    ) {
        this.video = video;
        this.startAt = startAt;
        this.endAt = endAt;
        this.peakCongestion = peakCongestion;
        this.avgDwellTimeSeconds = avgDwellTimeSeconds;
        this.totalCount = totalCount;
        this.weather = weather;
        this.temperature = temperature;
        this.rawData = rawData;
    }
}