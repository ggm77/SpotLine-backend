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

        return ResponseEntity.ok(analyticsService.getWeatherImpact(weatherImpactRequestDto));
    }

    @PostMapping("/analytics/weekday-patterns")
    public ResponseEntity<PerformanceResultResponseDto> getWeekdayPatterns(
            @RequestBody final WeekdayPatternRequestDto weekdayPatternRequestDto) {

        return ResponseEntity.ok(analyticsService.getWeekdayPatterns(weekdayPatternRequestDto));
    }

    @PostMapping("/analytics/visits/count")
    public ResponseEntity<VisitCountResponseDto> getVisitCount(
            @RequestBody final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {

        return ResponseEntity.ok(analyticsService.getVisitCount(defaultStartAtEndAtRequestDto));
    }

    @PostMapping("/analytics/predictions/tomorrow")
    public ResponseEntity<PredictionTomorrowResponseDto> getPredictionTomorrow() {
        return ResponseEntity.ok(analyticsService.getPredictionTomorrow());
    }

    @PostMapping("/analytics/predictions/next-week")
    public ResponseEntity<PredictionNextWeekResponseDto> getPredictionNextWeek() {
        return ResponseEntity.ok(analyticsService.getPredictionNextWeek());
    }

    @PostMapping("/analytics/daily-briefing")
    public ResponseEntity<MessageResponseDto> getDailyBriefing() {
        return ResponseEntity.ok(analyticsService.getDailyBriefing());
    }

    @PostMapping("/analytics/marketing-recommendations")
    public ResponseEntity<MessageResponseDto> getMarketingRecommendations() {
        return ResponseEntity.ok(analyticsService.getMarketingRecommendations());
    }
    @GetMapping("/analytics/visits/daily")
    public ResponseEntity<DailyVisitCountResponseDto> getDailyVisitCount(
            @RequestParam(value = "date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return ResponseEntity.ok(analyticsService.getDailyVisitCount(date));
    }
}
