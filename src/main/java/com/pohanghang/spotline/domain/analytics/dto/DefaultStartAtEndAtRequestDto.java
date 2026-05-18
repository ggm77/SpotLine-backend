package com.pohanghang.spotline.domain.analytics.dto;

import java.time.LocalDateTime;

public record DefaultStartAtEndAtRequestDto(
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
