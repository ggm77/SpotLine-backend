package com.pohanghang.spotline.global.infra.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenMeteoResponseDto(
        Current current
) {
    public record Current(
            @JsonProperty("temperature_2m") Double temperature2m,
            Double precipitation,
            @JsonProperty("weather_code") Integer weatherCode
    ) {}
}
