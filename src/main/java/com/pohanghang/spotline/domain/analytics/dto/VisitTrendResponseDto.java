package com.pohanghang.spotline.domain.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VisitTrendResponseDto(
        List<LocalDateTime> time, // x축 (시각)
        List<Integer> data // y축 (방문자 수)
) {
}
