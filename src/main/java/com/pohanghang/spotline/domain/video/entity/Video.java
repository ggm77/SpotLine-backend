package com.pohanghang.spotline.domain.video.entity;

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
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer visitorCount; // 방문자 수

    @Column(nullable = false)
    private Integer congestionLevel; // 혼잡도 (0 ~ 100)

    @Column(nullable = false)
    private Integer dwellTime; // 체류 시간 (분)

    @Column(nullable = false)
    private Integer manCount; // 남자 수

    @Column(nullable = false)
    private Integer womanCount; // 여자 수

    @Column(nullable = false)
    private Integer doorwayEventCount; // 출입문 이벤트 수

    @Column(nullable = false)
    private Integer counterEventCount; // 계산대 이벤트 수

    @Column(nullable = false)
    private List<Integer> ageCount; // 연령대별 수 (0~9세, 10대, 20대, ... 90대까지만)

    @Column(nullable = false)
    private LocalDateTime startAt; // 영상 시작 시점

    @Column(nullable = false)
    private LocalDateTime endAt; // 영상 끝나는 시점

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Weather weather; // 당시 날씨

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Video(
            Long id,
            Integer visitorCount,
            Integer congestionLevel,
            Integer dwellTime,
            Integer manCount,
            Integer womanCount,
            Integer doorwayEventCount,
            Integer counterEventCount,
            List<Integer> ageCount,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Weather weather
    ) {
        this.id = id;
        this.visitorCount = visitorCount;
        this.congestionLevel = congestionLevel;
        this.dwellTime = dwellTime;
        this.manCount = manCount;
        this.womanCount = womanCount;
        this.doorwayEventCount = doorwayEventCount;
        this.counterEventCount = counterEventCount;
        this.ageCount = ageCount;
        this.startAt = startAt;
        this.endAt = endAt;
        this.weather = weather;
    }
}
