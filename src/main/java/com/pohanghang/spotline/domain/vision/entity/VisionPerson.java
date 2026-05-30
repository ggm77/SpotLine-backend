package com.pohanghang.spotline.domain.vision.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비전 AI가 추적한 개별 방문자 한 명의 입·퇴장 및 체류 정보.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vision_person")
public class VisionPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vision_data_id", nullable = false)
    private VisionData visionData;

    private Long trackId; // 비전 AI가 부여한 추적 id

    private Integer age; // 나이대 (10, 20, ...)

    private Integer gender; // 1 = 남자, 2 = 여자

    private LocalDateTime inAt; // 입장 시점

    private LocalDateTime outAt; // 퇴장 시점 (아직 매장에 있으면 null)

    private Integer dwellTime; // 체류 시간 (초)

    @Builder
    public VisionPerson(
            final VisionData visionData,
            final Long trackId,
            final Integer age,
            final Integer gender,
            final LocalDateTime inAt,
            final LocalDateTime outAt,
            final Integer dwellTime
    ) {
        this.visionData = visionData;
        this.trackId = trackId;
        this.age = age;
        this.gender = gender;
        this.inAt = inAt;
        this.outAt = outAt;
        this.dwellTime = dwellTime;
    }
}
