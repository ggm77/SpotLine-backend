package com.pohanghang.spotline.domain.analytics.dto;

import java.util.List;

public record VisitCountResponseDto(
        List<String> date,
        List<List<Integer>> data
) {
}
