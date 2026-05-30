package com.pohanghang.spotline.domain.analytics.model;

import com.pohanghang.spotline.domain.analytics.entity.Weather;

import java.time.LocalDateTime;

/**
 * 통계 계산기들이 사용하는 한 구간(스냅샷)의 방문/날씨 정보.
 * 비전 데이터({@code VisionData})로부터 만들어진다.
 */
public class AnalyticsRow {

    private final LocalDateTime startAt;
    private final Integer totalCount;
    private final Weather weather;
    private final Double temperature;

    public AnalyticsRow(
            final LocalDateTime startAt,
            final Integer totalCount,
            final Weather weather,
            final Double temperature
    ) {
        this.startAt = startAt;
        this.totalCount = totalCount;
        this.weather = weather;
        this.temperature = temperature;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public Weather getWeather() {
        return weather;
    }

    public Double getTemperature() {
        return temperature;
    }
}
