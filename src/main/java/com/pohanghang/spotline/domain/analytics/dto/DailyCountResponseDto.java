package com.pohanghang.spotline.domain.analytics.dto;

public record DailyCountResponseDto(
        Integer count,
        Integer avgCount
) { }
