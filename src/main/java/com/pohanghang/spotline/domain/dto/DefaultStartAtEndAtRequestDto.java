package com.pohanghang.spotline.domain.dto;

import java.time.LocalDateTime;

public record DefaultStartAtEndAtRequestDto(
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
