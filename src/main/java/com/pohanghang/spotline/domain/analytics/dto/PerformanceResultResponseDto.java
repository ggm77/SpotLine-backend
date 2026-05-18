package com.pohanghang.spotline.domain.analytics.dto;

import com.pohanghang.spotline.domain.video.entity.PerformanceResult;

public record PerformanceResultResponseDto(
        Float realValue,
        Float expectValue,
        PerformanceResult result
) {
}
