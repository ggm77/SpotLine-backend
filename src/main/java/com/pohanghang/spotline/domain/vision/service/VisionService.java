package com.pohanghang.spotline.domain.vision.service;

import com.pohanghang.spotline.domain.analytics.entity.Weather;
import com.pohanghang.spotline.domain.vision.dto.VisionDataPersonDto;
import com.pohanghang.spotline.domain.vision.dto.VisionDataRequestDto;
import com.pohanghang.spotline.domain.vision.entity.VisionData;
import com.pohanghang.spotline.domain.vision.entity.VisionPerson;
import com.pohanghang.spotline.domain.vision.repository.VisionDataRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.openmeteo.OpenMeteoClient;
import com.pohanghang.spotline.global.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisionService {

    private final VisionDataRepository visionDataRepository;
    private final OpenMeteoClient openMeteoClient;

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

        // 3) 촬영 시점 날씨 조회 (통계 보정에 사용). 외부 API 실패 시 기본값으로 대체.
        final OpenMeteoClient.WeatherData weatherData = resolveWeather(capturedAt);

        // 4) 스냅샷 엔티티 생성
        final VisionData visionData = VisionData.builder()
                .totalCount(visionDataRequestDto.totalCount())
                .maxResponseWaitTime(visionDataRequestDto.maxResponseWaitTime())
                .maxEmptyTableTime(visionDataRequestDto.maxEmptyTableTime())
                .coreCustomerAge(visionDataRequestDto.coreCustomerAge())
                .coreCustomerGender(visionDataRequestDto.coreCustomerGender())
                .avgDwellTime(visionDataRequestDto.avgDwellTime())
                .justLeftCount(visionDataRequestDto.justLeftCount())
                .capturedAt(capturedAt)
                .endAt(endAt)
                .weather(weatherData.weather())
                .temperature(weatherData.temperature())
                .build();

        // 5) 방문자(사람) 정보 매핑
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

        // 6) 저장 (people 은 cascade 로 함께 저장)
        visionDataRepository.save(visionData);
    }

    private OpenMeteoClient.WeatherData resolveWeather(final LocalDateTime capturedAt) {
        try {
            return openMeteoClient.getSeoulWeatherData(capturedAt);
        } catch (Exception ex) {
            log.warn("날씨 API 호출 실패 (기본값 대체): {}. 기본 날씨 정보(흐림, 18.0도)로 저장합니다.", ex.getMessage());
            return new OpenMeteoClient.WeatherData(18.0, 0.0, Weather.CLOUDY);
        }
    }
}
