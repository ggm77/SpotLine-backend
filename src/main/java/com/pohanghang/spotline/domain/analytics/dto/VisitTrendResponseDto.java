package com.pohanghang.spotline.domain.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VisitTrendResponseDto(
        List<LocalDateTime> time,
        List<Integer> data
) { }
