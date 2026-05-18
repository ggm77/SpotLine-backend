package com.pohanghang.spotline.domain.analytics.service;

import com.pohanghang.spotline.domain.analytics.dto.*;
import com.pohanghang.spotline.domain.analytics.entity.AgeGroup;
import com.pohanghang.spotline.domain.analytics.entity.Analytics;
import com.pohanghang.spotline.domain.analytics.repository.AnalyticsRepository;
import com.pohanghang.spotline.domain.analytics.util.WeatherImpactCalculator;
import com.pohanghang.spotline.domain.video.entity.Video;
import com.pohanghang.spotline.domain.video.repository.VideoRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Map<AgeGroup, String> AGE_GROUP_LABELS = Map.of(
            AgeGroup.CHILD, "00s",
            AgeGroup.TEN, "10s",
            AgeGroup.TWENTY, "20s",
            AgeGroup.THIRTY, "30s",
            AgeGroup.FORTY, "40s",
            AgeGroup.FIFTY_PLUS, "50s+",
            AgeGroup.UNKNOWN, "UNKNOWN"
    );

    private final AnalyticsRepository analyticsRepository;
    private final VideoRepository videoRepository;

    public RawAnalyticsDto getRawAnalytics(final Long videoId) {
        // 1) null 검사
        if (videoId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) video 조회
        final Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new CustomException(ExceptionCode.VIDEO_NOT_FOUND));

        // 3) video로 analytics 조회
        final Analytics analytics = analyticsRepository.findByVideo(video)
                .orElseThrow(() -> new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND));

        // 4) 문자열 파싱
        return JsonUtil.toObject(analytics.getRawData(), RawAnalyticsDto.class);
    }

    @Transactional(readOnly = true)
    public CoreCustomerResponseDto getCoreCustomers(final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {
        // 1) null 검사
        if (defaultStartAtEndAtRequestDto == null
                || defaultStartAtEndAtRequestDto.startAt() == null
                || defaultStartAtEndAtRequestDto.endAt() == null
                || !defaultStartAtEndAtRequestDto.startAt().isBefore(defaultStartAtEndAtRequestDto.endAt())) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final LocalDateTime startAt = defaultStartAtEndAtRequestDto.startAt();
        final LocalDateTime endAt = defaultStartAtEndAtRequestDto.endAt();

        final List<AnalyticsRepository.CoreCustomerGroup> coreCustomerGroups =
                analyticsRepository.findCoreCustomerGroups(startAt, endAt);

        return coreCustomerGroups.stream()
                .findFirst()
                .map(coreCustomerGroup -> new CoreCustomerResponseDto(
                        coreCustomerGroup.getGender().name(),
                        AGE_GROUP_LABELS.get(coreCustomerGroup.getAgeGroup())
                ))
                .orElseThrow(() -> new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public AgeGroupDistributionDto getHourlyPopulation(final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {
        // 1) null 검사
        if (defaultStartAtEndAtRequestDto == null
                || defaultStartAtEndAtRequestDto.startAt() == null
                || defaultStartAtEndAtRequestDto.endAt() == null
                || !defaultStartAtEndAtRequestDto.startAt().isBefore(defaultStartAtEndAtRequestDto.endAt())) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final LocalDateTime startAt = defaultStartAtEndAtRequestDto.startAt();
        final LocalDateTime endAt = defaultStartAtEndAtRequestDto.endAt();

        final Map<AgeGroup, Integer> ageGroupCounts = new EnumMap<>(AgeGroup.class);
        for (AgeGroup ageGroup : AgeGroup.values()) {
            ageGroupCounts.put(ageGroup, 0);
        }

        final List<AnalyticsRepository.HourlyPopulationGroup> hourlyPopulationGroups =
                analyticsRepository.findHourlyPopulationGroups(startAt, endAt);
        for (AnalyticsRepository.HourlyPopulationGroup hourlyPopulationGroup : hourlyPopulationGroups) {
            ageGroupCounts.put(
                    hourlyPopulationGroup.getAgeGroup(),
                    Math.toIntExact(hourlyPopulationGroup.getTotalCount())
            );
        }

        return new AgeGroupDistributionDto(
                ageGroupCounts.get(AgeGroup.CHILD),
                ageGroupCounts.get(AgeGroup.TEN),
                ageGroupCounts.get(AgeGroup.TWENTY),
                ageGroupCounts.get(AgeGroup.THIRTY),
                ageGroupCounts.get(AgeGroup.FORTY),
                ageGroupCounts.get(AgeGroup.FIFTY_PLUS)
        );
    }

    @Transactional(readOnly = true)
    public PerformanceResultResponseDto getWeatherImpact(final WeatherImpactRequestDto weatherImpactRequestDto) {
        if (weatherImpactRequestDto == null || weatherImpactRequestDto.day() == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final List<AnalyticsRepository.WeatherImpactRow> weatherImpactRows = analyticsRepository.findWeatherImpactRows();
        return WeatherImpactCalculator.calculate(weatherImpactRequestDto, weatherImpactRows);
    }
}
