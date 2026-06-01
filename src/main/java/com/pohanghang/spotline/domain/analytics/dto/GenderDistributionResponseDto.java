package com.pohanghang.spotline.domain.analytics.dto;

public record GenderDistributionResponseDto(
        int male,   // 남성 수 (-1 = 데이터 없음)
        int female  // 여성 수 (-1 = 데이터 없음)
) {
}
