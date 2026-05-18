package com.pohanghang.spotline.domain.dto;

import java.time.LocalDateTime;

public record WeekdayPatternRequestDto(
        LocalDateTime day,
        Integer dayOfWeek
) {
}
