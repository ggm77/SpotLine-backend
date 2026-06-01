package com.pohanghang.spotline.domain.analytics.controller;

import com.pohanghang.spotline.domain.analytics.dto.CoreCustomerV2ResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.CountResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.DailyCountResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.DailySalesResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.GenderDistributionResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.MenuResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.TimeResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.VisitTrendResponseDto;
import com.pohanghang.spotline.domain.analytics.service.AnalyticsV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v2/analytics")
@RequiredArgsConstructor
public class AnalyticsV2Controller {

    private final AnalyticsV2Service analyticsV2Service;

    // 지금 몇 명 있나
    @GetMapping("/current-count")
    public ResponseEntity<CountResponseDto> getCurrentCount() {
        return ResponseEntity.ok(analyticsV2Service.getCurrentCount());
    }

    // 몇 시가 가장 바쁜가
    @GetMapping("/peek-time")
    public ResponseEntity<TimeResponseDto> getPeakTime(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getPeakTime(startAt, endAt));
    }

    // 오늘 매출 얼마인가
    @GetMapping("/daily-sales")
    public ResponseEntity<DailySalesResponseDto> getDailySales(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getDailySales(startAt, endAt));
    }

    // 어떤 메뉴가 잘 팔리나
    @GetMapping("/best-menu")
    public ResponseEntity<MenuResponseDto> getBestMenu(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getBestMenu(startAt, endAt));
    }

    // 최대 응대 대기 시간
    @GetMapping("/response-wait-time")
    public ResponseEntity<TimeResponseDto> getResponseWaitTime(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getResponseWaitTime(startAt, endAt));
    }

    // 그냥 나간 손님 수
    @GetMapping("/just-left-count")
    public ResponseEntity<CountResponseDto> getJustLeftCount(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getJustLeftCount(startAt, endAt));
    }

    // 최대 테이블 유휴 시간
    @GetMapping("/empty-table-time")
    public ResponseEntity<TimeResponseDto> getEmptyTableTime(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getEmptyTableTime(startAt, endAt));
    }

    // 평균과 비교해서 오늘 얼마나 왔는지
    @GetMapping("/daily-count")
    public ResponseEntity<DailyCountResponseDto> getDailyCount(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getDailyCount(startAt, endAt));
    }

    // 매장 방문 추세
    @GetMapping("/visit-trend")
    public ResponseEntity<VisitTrendResponseDto> getVisitTrend(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getVisitTrend(startAt, endAt));
    }

    // 핵심 고객
    @GetMapping("/core-customer")
    public ResponseEntity<CoreCustomerV2ResponseDto> getCoreCustomer(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getCoreCustomer(startAt, endAt));
    }

    // 평균 체류시간
    @GetMapping("/avg-dwell")
    public ResponseEntity<TimeResponseDto> getAvgDwell(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getAvgDwell(startAt, endAt));
    }

    // 성별 분포
    @GetMapping("/gender-distribution")
    public ResponseEntity<GenderDistributionResponseDto> getGenderDistribution(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startAt,
            @RequestParam("endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endAt
    ) {
        return ResponseEntity.ok(analyticsV2Service.getGenderDistribution(startAt, endAt));
    }
}
