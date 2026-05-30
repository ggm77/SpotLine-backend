package com.pohanghang.spotline.domain.analytics.dto;

public record CoreCustomerV2ResponseDto(
        Integer age, // 핵심 고객 나이대 (10, 20, 30, ...)
        Integer gender // 핵심 고객 성별 (1 = 남자, 2 = 여자)
) {
}
