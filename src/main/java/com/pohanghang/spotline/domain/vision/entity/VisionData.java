package com.pohanghang.spotline.domain.vision.entity;

import com.pohanghang.spotline.domain.analytics.entity.Weather;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 비전 AI가 분석해서 보내주는 한 구간(스냅샷)의 집계 데이터.
 * {@code capturedAt} ~ {@code endAt} 구간 동안 분석된 결과를 담는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vision_data", indexes = {
        @Index(name = "idx_vision_data_captured", columnList = "capturedAt, endAt")
})
public class VisionData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer totalCount; // 구간 내 총 방문자 수

    private Integer maxResponseWaitTime; // 최대 응대 대기 시간 (분)

    private Integer maxEmptyTableTime; // 최대 테이블 유휴 시간 (분)

    private Integer coreCustomerAge; // 핵심 고객 나이대 (10, 20, ...)

    private Integer coreCustomerGender; // 핵심 고객 성별 (1 = 남자, 2 = 여자)

    private Integer avgDwellTime; // 평균 체류시간 (분)

    private Integer justLeftCount; // 그냥 나간 손님 수

    @Column(nullable = false)
    private LocalDateTime capturedAt; // 원본 데이터 촬영 시작 시점

    @Column(nullable = false)
    private LocalDateTime endAt; // 원본 데이터 끝나는 시점

    @Enumerated(EnumType.STRING)
    private Weather weather; // 촬영 시점 날씨 (수집 시 보정용으로 함께 저장)

    private Double temperature; // 촬영 시점 기온(℃)

    @OneToMany(mappedBy = "visionData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VisionPerson> people = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder
    public VisionData(
            final Integer totalCount,
            final Integer maxResponseWaitTime,
            final Integer maxEmptyTableTime,
            final Integer coreCustomerAge,
            final Integer coreCustomerGender,
            final Integer avgDwellTime,
            final Integer justLeftCount,
            final LocalDateTime capturedAt,
            final LocalDateTime endAt,
            final Weather weather,
            final Double temperature
    ) {
        this.totalCount = totalCount;
        this.maxResponseWaitTime = maxResponseWaitTime;
        this.maxEmptyTableTime = maxEmptyTableTime;
        this.coreCustomerAge = coreCustomerAge;
        this.coreCustomerGender = coreCustomerGender;
        this.avgDwellTime = avgDwellTime;
        this.justLeftCount = justLeftCount;
        this.capturedAt = capturedAt;
        this.endAt = endAt;
        this.weather = weather;
        this.temperature = temperature;
    }

    public void addPerson(final VisionPerson person) {
        this.people.add(person);
    }
}
