package com.pohanghang.spotline.domain.analytics.controller;

import com.pohanghang.spotline.domain.analytics.dto.*;
import com.pohanghang.spotline.domain.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analytics/core-customers")
    public ResponseEntity<CoreCustomerResponseDto> getCoreCustomers(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt) {

        return ResponseEntity.ok(analyticsService.getCoreCustomers(new DefaultStartAtEndAtRequestDto(startAt, endAt)));
    }

    @GetMapping("/analytics/hourly-population")
    public ResponseEntity<AgeGroupDistributionDto> getHourlyPopulation(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt) {

        return ResponseEntity.ok(analyticsService.getHourlyPopulation(new DefaultStartAtEndAtRequestDto(startAt, endAt)));
    }

    @GetMapping("/analytics/weather-impact")
    public ResponseEntity<PerformanceResultResponseDto> getWeatherImpact(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt) {

        return ResponseEntity.ok(analyticsService.getWeatherImpact(new WeatherImpactRequestDto(startAt)));
    }

    @GetMapping("/analytics/weekday-patterns")
    public ResponseEntity<PerformanceResultResponseDto> getWeekdayPatterns(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt) {

        return ResponseEntity.ok(analyticsService.getWeekdayPatterns(
                new WeekdayPatternRequestDto(startAt, startAt.getDayOfWeek().getValue())));
    }

    @GetMapping("/analytics/visits/count")
    public ResponseEntity<VisitCountResponseDto> getVisitCount(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt) {

        return ResponseEntity.ok(analyticsService.getVisitCount(new DefaultStartAtEndAtRequestDto(startAt, endAt)));
    }

    @GetMapping("/analytics/predictions/tomorrow")
    public ResponseEntity<PredictionTomorrowResponseDto> getPredictionTomorrow() {
        return ResponseEntity.ok(analyticsService.getPredictionTomorrow());
    }

    @GetMapping("/analytics/predictions/next-week")
    public ResponseEntity<PredictionNextWeekResponseDto> getPredictionNextWeek() {
        return ResponseEntity.ok(analyticsService.getPredictionNextWeek());
    }

    @GetMapping("/analytics/daily-briefing")
    public ResponseEntity<MessageResponseDto> getDailyBriefing() {
        return ResponseEntity.ok(analyticsService.getDailyBriefing());
    }

    @GetMapping("/analytics/marketing-recommendations")
    public ResponseEntity<MessageResponseDto> getMarketingRecommendations() {
        return ResponseEntity.ok(analyticsService.getMarketingRecommendations());
    }

    @GetMapping("/analytics/visits/daily")
    public ResponseEntity<DailyVisitCountResponseDto> getDailyVisitCount(
            @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date) {
        return ResponseEntity.ok(analyticsService.getDailyVisitCount(date));
    }
}
