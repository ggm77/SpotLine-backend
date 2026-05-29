package com.pohanghang.spotline.domain.analytics.controller;

import com.pohanghang.spotline.domain.analytics.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class AnalyticsController {

    // 지금 몇 명 있나
    @GetMapping("/analytics/current-count")
    public ResponseEntity<CountResponseDto> getCurrentCount() {

        // mock
        return ResponseEntity.ok(new CountResponseDto(10));
    }

    // 몇 시가 가장 바쁜가
    @GetMapping("/analytics/peek-time")
    public ResponseEntity<TimeResponseDto> getPeakTime(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new TimeResponseDto(18));
    }

    // 오늘 매출 얼마인가
    @GetMapping("/analytics/daily-sales")
    public ResponseEntity<DailySalesResponseDto> getDailySales(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new DailySalesResponseDto(18));
    }

    // 어떤 메뉴가 잘 팔리나
    @GetMapping("/analytics/best-menu")
    public ResponseEntity<MenuResponseDto> getBestMenu(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new MenuResponseDto("삼겹살"));
    }

    // 최대 응대 대기 시간
    @GetMapping("/analytics/response-wait-time")
    public ResponseEntity<TimeResponseDto> getResponseWaitTime(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new TimeResponseDto(3));
    }

    // 그냥 나간 손님 수
    @GetMapping("/analytics/just-left-count")
    public ResponseEntity<CountResponseDto> getJustLeftCount(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new CountResponseDto(2));
    }

    // 최대 테이블 유휴 시간
    @GetMapping("/analytics/empty-table-time")
    public ResponseEntity<TimeResponseDto> getEmptyTableTime(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new TimeResponseDto(20));
    }

    // 평균과 비교해서 오늘 얼마나 왔는지
    @GetMapping("/analytics/daily-count")
    public ResponseEntity<DailyCountResponseDto> getDailyCount(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new DailyCountResponseDto(30, 25));
    }

    // 매장 방문 추세
    @GetMapping("/analytics/visit-trend")
    public ResponseEntity<VisitTrendResponseDto> getVisitTrend(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new VisitTrendResponseDto(
                Arrays.asList(LocalDateTime.now()),
                Arrays.asList(30)
        ));
    }

    // 핵심 고객
    @GetMapping("/analytics/core-customer")
    public ResponseEntity<CoreCustomerResponseDto> getCoreCustomer(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new CoreCustomerResponseDto(20, 1));
    }

    // 평균 체류 시간
    @GetMapping("/analytics/avg-dwell")
    public ResponseEntity<TimeResponseDto> getAvgDwellTime(
            @RequestParam("startAt") final LocalDateTime startAt,
            @RequestParam("endAt") final LocalDateTime endAt
    ) {

        // mock
        return ResponseEntity.ok(new TimeResponseDto(20));
    }
}
