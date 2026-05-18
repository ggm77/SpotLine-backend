package com.pohanghang.spotline.domain.analytics.dto;

import java.time.LocalDateTime;

public record WeatherImpactRequestDto(
        LocalDateTime day,
        Integer rain,
        Float temp
) {
}
