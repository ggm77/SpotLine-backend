package com.pohanghang.spotline.domain.vision.service;

import com.pohanghang.spotline.domain.vision.dto.VisionDataPersonDto;
import com.pohanghang.spotline.domain.vision.dto.VisionDataRequestDto;
import com.pohanghang.spotline.domain.vision.entity.VisionData;
import com.pohanghang.spotline.domain.vision.entity.VisionPerson;
import com.pohanghang.spotline.domain.vision.repository.VisionDataRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VisionService {

    private final VisionDataRepository visionDataRepository;

    @Transactional
    public void saveVisionData(final VisionDataRequestDto visionDataRequestDto) {
        // 1) 필수 값 검사
        if (visionDataRequestDto == null || visionDataRequestDto.totalCount() == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 시각 파싱 ("2026-05-17T15:00:00" / "...Z" 두 형식 모두 허용)
        final LocalDateTime capturedAt = DateTimeUtil.parseFlexible(visionDataRequestDto.capturedAt());
        final LocalDateTime endAt = DateTimeUtil.parseFlexible(visionDataRequestDto.endAt());
        if (capturedAt == null || endAt == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 3) 스냅샷 엔티티 생성
        final VisionData visionData = VisionData.builder()
                .totalCount(visionDataRequestDto.totalCount())
                .peakTime(visionDataRequestDto.peakTime())
                .maxResponseWaitTime(visionDataRequestDto.maxResponseWaitTime())
                .maxEmptyTableTime(visionDataRequestDto.maxEmptyTableTime())
                .coreCustomerAge(visionDataRequestDto.coreCustomerAge())
                .coreCustomerGender(visionDataRequestDto.coreCustomerGender())
                .avgDwellTime(visionDataRequestDto.avgDwellTime())
                .justLeftCount(visionDataRequestDto.justLeftCount())
                .capturedAt(capturedAt)
                .endAt(endAt)
                .build();

        // 4) 방문자(사람) 정보 매핑
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
                        .inAt(DateTimeUtil.parseFlexible(person.inAt()))
                        .outAt(DateTimeUtil.parseFlexible(person.outAt()))
                        .dwellTime(person.dwellTime())
                        .build());
            }
        }

        // 5) 저장 (people 은 cascade 로 함께 저장)
        visionDataRepository.save(visionData);
    }
}
