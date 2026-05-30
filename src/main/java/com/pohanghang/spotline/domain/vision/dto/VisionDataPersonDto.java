package com.pohanghang.spotline.domain.vision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record VisionDataPersonDto(
        Long id, // 비전 AI가 부여한 추적 id
        Integer age, // 나이대 (10, 20, ...)
        Integer gender, // 1 = 남자, 2 = 여자
        @JsonProperty("in") OffsetDateTime inAt, // 입장 시점
        @JsonProperty("out") OffsetDateTime outAt, // 퇴장 시점
        Integer dwellTime // 체류 시간 (초)
) {
}
