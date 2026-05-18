package com.pohanghang.spotline.domain.dto;

import java.util.List;

public record VisitCountResponseDto(
        List<String> date,
        List<List<Integer>> data
) {
}
