package com.pohanghang.spotline.domain.vision.service;

import com.pohanghang.spotline.domain.vision.dto.VisionDataPersonDto;
import com.pohanghang.spotline.domain.vision.dto.VisionDataRequestDto;
import com.pohanghang.spotline.domain.vision.entity.VisionData;
import com.pohanghang.spotline.domain.vision.entity.VisionPerson;
import com.pohanghang.spotline.domain.vision.repository.VisionDataRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class VisionService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final VisionDataRepository visionDataRepository;

    @Transactional
    public void saveVisionData(final VisionDataRequestDto visionDataRequestDto) {
        // 1) 필수 값 검사
        if (visionDataRequestDto == null
                || visionDataRequestDto.totalCount() == null
                || visionDataRequestDto.capturedAt() == null
                || visionDataRequestDto.endAt() == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 스냅샷 엔티티 생성
        final VisionData visionData = VisionData.builder()
                .totalCount(visionDataRequestDto.totalCount())
                .peakTime(visionDataRequestDto.peakTime())
                .maxResponseWaitTime(visionDataRequestDto.maxResponseWaitTime())
                .maxEmptyTableTime(visionDataRequestDto.maxEmptyTableTime())
                .coreCustomerAge(visionDataRequestDto.coreCustomerAge())
                .coreCustomerGender(visionDataRequestDto.coreCustomerGender())
                .avgDwellTime(visionDataRequestDto.avgDwellTime())
                .justLeftCount(visionDataRequestDto.justLeftCount())
                .capturedAt(toLocalDateTime(visionDataRequestDto.capturedAt()))
                .endAt(toLocalDateTime(visionDataRequestDto.endAt()))
                .build();

        // 3) 방문자(사람) 정보 매핑
        if (visionDataRequestDto.people() != null) {
            for (VisionDataPersonDto person : visionDataRequestDto.people()) {
                if (person == null) {
                    continue;
                }
                visionData.addPerson(VisionPerson.builder()
                        .visionData(visionData)
                        .trackId(person.id())
                        .age(person.age())
                        .gender(person.gender())
                        .inAt(toLocalDateTime(person.inAt()))
                        .outAt(toLocalDateTime(person.outAt()))
                        .dwellTime(person.dwellTime())
                        .build());
            }
        }

        // 4) 저장 (people 은 cascade 로 함께 저장)
        visionDataRepository.save(visionData);
    }

    /**
     * UTC(Z) 기준으로 들어온 시각을 한국 시간(Asia/Seoul) 기준 LocalDateTime 으로 변환한다.
     * 통계 조회 파라미터(startAt/endAt)가 한국 시간 기준이므로 동일 기준으로 맞춘다.
     */
    private LocalDateTime toLocalDateTime(final OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(KST).toLocalDateTime();
    }
}
