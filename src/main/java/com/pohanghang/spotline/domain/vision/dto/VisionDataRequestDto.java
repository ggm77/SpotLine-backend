package com.pohanghang.spotline.domain.vision.dto;

import java.util.List;

public record VisionDataRequestDto(
        Integer totalCount, // 총 방문자 수
        Integer peakTime, // 가장 바쁜 시각 (시)
        Integer maxResponseWaitTime, // 최대 응대 대기 시간 (분)
        List<VisionDataPersonDto> people, // 방문자별 입·퇴장 정보
        Integer maxEmptyTableTime, // 최대 테이블 유휴 시간 (분)
        Integer coreCustomerAge, // 핵심 고객 나이대 (10, 20, ...)
        Integer coreCustomerGender, // 핵심 고객 성별 (1 = 남자, 2 = 여자)
        Integer avgDwellTime, // 평균 체류시간 (분)
        Integer justLeftCount, // 그냥 나간 손님 수
        String capturedAt, // 원본 데이터 촬영 시작 시점 ("2026-05-17T15:00:00" 또는 "...Z" 형식 모두 허용)
        String endAt // 원본 데이터 끝나는 시점
) {
}
