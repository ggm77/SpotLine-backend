package com.pohanghang.spotline.domain.vision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VisionDataPersonDto(
        Long id, // 비전 AI가 부여한 추적 id
        Integer age, // 나이대 (10, 20, ...)
        Integer gender, // 1 = 남자, 2 = 여자
        @JsonProperty("in") String inAt, // 입장 시점 ("2026-05-17T15:00:00" 또는 "...Z" 형식 모두 허용)
        @JsonProperty("out") String outAt, // 퇴장 시점
        Integer dwellTime // 체류 시간 (초)
) {
}
