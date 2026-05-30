package com.pohanghang.spotline.domain.analytics.dto;

import java.util.List;

public record PredictionNextWeekResponseDto(
        List<PredictionTomorrowResponseDto> result
) {
}
