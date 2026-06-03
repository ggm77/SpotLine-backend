package com.pohanghang.spotline.domain.vision.dto;

import com.pohanghang.spotline.domain.analytics.entity.Weather;
import com.pohanghang.spotline.domain.vision.entity.VisionData;

import java.time.LocalDateTime;

public record VisionDataResponseDto(
        Long id,
        Integer totalCount, // 구간 내 총 방문자 수
        Integer maxResponseWaitTime, // 최대 응대 대기 시간 (분)
        Integer maxEmptyTableTime, // 최대 테이블 유휴 시간 (분)
        Integer coreCustomerAge, // 핵심 고객 나이대 (10, 20, ...)
        Integer coreCustomerGender, // 핵심 고객 성별 (1 = 남자, 2 = 여자)
        Integer avgDwellTime, // 평균 체류시간 (분)
        Integer justLeftCount, // 그냥 나간 손님 수
        LocalDateTime capturedAt, // 원본 데이터 촬영 시작 시점
        LocalDateTime endAt, // 원본 데이터 끝나는 시점
        Weather weather, // 촬영 시점 날씨
        Double temperature, // 촬영 시점 기온(℃)
        LocalDateTime createdAt // DB 등록 시점
) {
    public static VisionDataResponseDto from(final VisionData visionData) {
        return new VisionDataResponseDto(
                visionData.getId(),
                visionData.getTotalCount(),
                visionData.getMaxResponseWaitTime(),
                visionData.getMaxEmptyTableTime(),
                visionData.getCoreCustomerAge(),
                visionData.getCoreCustomerGender(),
                visionData.getAvgDwellTime(),
                visionData.getJustLeftCount(),
                visionData.getCapturedAt(),
                visionData.getEndAt(),
                visionData.getWeather(),
                visionData.getTemperature(),
                visionData.getCreatedAt()
        );
    }
}
