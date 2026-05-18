package com.pohanghang.spotline.domain.dto;

import java.time.LocalDateTime;

public record WeatherImpactRequestDto(
        LocalDateTime day,
        Integer rain,
        Float temp
) {
}
