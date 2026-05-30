package com.pohanghang.spotline.domain.analytics.dto;

public record DailyCountResponseDto(
        Integer count, // 오늘 온 사람들 수
        Integer avgCount // 평균 온 사람들 수
) {
}
