package com.pohanghang.spotline.domain.analytics.dto;

import java.time.LocalDateTime;

public record WeekdayPatternRequestDto(
        LocalDateTime day,
        Integer dayOfWeek
) {
}
