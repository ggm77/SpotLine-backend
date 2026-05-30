package com.pohanghang.spotline.domain.analytics.dto;

public record PredictionTomorrowResponseDto(
        Integer expectedVisits, // 예상 방문자 수
        Integer minVisits, // 신뢰구간 최소
        Integer maxVisits // 신뢰구간 최대
) {
}
