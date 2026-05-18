package com.pohanghang.spotline.domain.dto;

import com.pohanghang.spotline.domain.entity.PerformanceResult;

public record PerformanceResultResponseDto(
        Float realValue,
        Float expectValue,
        PerformanceResult result
) {
}
