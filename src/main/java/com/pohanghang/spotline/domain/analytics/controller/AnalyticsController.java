package com.pohanghang.spotline.domain.analytics.controller;

import com.pohanghang.spotline.domain.analytics.dto.*;
import com.pohanghang.spotline.domain.analytics.service.AnalyticsService;
import com.pohanghang.spotline.domain.video.entity.PerformanceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analytics/raw")
    public ResponseEntity<RawAnalyticsDto> getRawAnalytics(
            @RequestParam(value = "videoId") final Long videoId) {

        return ResponseEntity.ok(analyticsService.getRawAnalytics(videoId));
    }

    @PostMapping("/analytics/core-customers")
    public ResponseEntity<CoreCustomerResponseDto> getCoreCustomers(
            @RequestBody final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {

        return ResponseEntity.ok(analyticsService.getCoreCustomers(defaultStartAtEndAtRequestDto));
    }

    @PostMapping("/analytics/hourly-population")
    public ResponseEntity<AgeGroupDistributionDto> getHourlyPopulation(
            @RequestBody final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {

        return ResponseEntity.ok(analyticsService.getHourlyPopulation(defaultStartAtEndAtRequestDto));
    }

    @PostMapping("/analytics/weather-impact")
    public ResponseEntity<PerformanceResultResponseDto> getWeatherImpact(
            @RequestBody final WeatherImpactRequestDto weatherImpactRequestDto) {

        // mock
        return ResponseEntity.ok(analyticsService.getWeatherImpact(weatherImpactRequestDto));
    }

    @PostMapping("/analytics/weekday-patterns")
    public ResponseEntity<PerformanceResultResponseDto> getWeekdayPatterns(
            @RequestBody final WeekdayPatternRequestDto weekdayPatternRequestDto) {

        // mock
        return ResponseEntity.ok(new PerformanceResultResponseDto(72.3f, 80.0f, PerformanceResult.BAD));
    }

    @PostMapping("/analytics/visits/count")
    public ResponseEntity<VisitCountResponseDto> getVisitCount(
            @RequestBody final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {

        // mock
        return ResponseEntity.ok(new VisitCountResponseDto(
                Arrays.asList("2026-05-17T15:00:00", "2026-05-17T16:00:00", "2026-05-17T17:00:00"),
                Arrays.asList(
                        Arrays.asList(10, 15, 20),
                        Arrays.asList(12, 18, 25),
                        Arrays.asList(8, 14, 22),
                        Arrays.asList(11, 16, 21))));
    }

    @PostMapping("/analytics/predictions/tomorrow")
    public ResponseEntity<PredictionTomorrowResponseDto> getPredictionTomorrow() {

        // mock
        return ResponseEntity.ok(new PredictionTomorrowResponseDto(142));
    }

    @PostMapping("/analytics/predictions/next-week")
    public ResponseEntity<PredictionNextWeekResponseDto> getPredictionNextWeek() {

        // mock
        return ResponseEntity.ok(new PredictionNextWeekResponseDto(
                Arrays.asList(120, 135, 150, 140, 160, 200, 180)));
    }

    @PostMapping("/analytics/daily-briefing")
    public ResponseEntity<MessageResponseDto> getDailyBriefing() {

        // mock
        return ResponseEntity.ok(new MessageResponseDto(
                "오늘 총 방문자 수는 142명으로, 전일 대비 12% 증가했습니다. 오후 2~4시 사이에 피크 타임이 형성되었으며, 20대 여성 고객의 비중이 가장 높았습니다."));
    }

    @PostMapping("/analytics/marketing-recommendations")
    public ResponseEntity<MessageResponseDto> getMarketingRecommendations() {

        // mock
        return ResponseEntity.ok(new MessageResponseDto(
                "20대 여성 고객 비율이 높으므로, SNS 기반 프로모션과 인스타그램 이벤트를 추천드립니다. 오후 2~4시 피크 타임에 맞춘 타임 세일도 효과적일 것으로 보입니다."));
    }
}
