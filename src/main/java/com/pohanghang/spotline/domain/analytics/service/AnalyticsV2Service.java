package com.pohanghang.spotline.domain.analytics.service;

import com.pohanghang.spotline.domain.analytics.dto.CoreCustomerV2ResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.CountResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.DailyCountResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.DailySalesResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.MenuResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.TimeResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.VisitTrendResponseDto;
import com.pohanghang.spotline.domain.vision.entity.VisionData;
import com.pohanghang.spotline.domain.vision.entity.VisionPerson;
import com.pohanghang.spotline.domain.vision.repository.VisionDataRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * v2 통계 API. 비전 AI가 적재한 {@link VisionData} 스냅샷들을 집계한다.
 *
 * <p>모든 필드는 null 로 들어올 수 있으므로, 집계 시에는 null 을 제외하고 계산하며
 * 단일 대표값이 필요한 경우(핵심 고객 등)에는 null 을 제외한 최신 값으로 대체한다.</p>
 */
@Service
@RequiredArgsConstructor
public class AnalyticsV2Service {

    // POS(토스) 연동 전까지 사용하는 임시 값
    private static final int ESTIMATED_SPEND_PER_VISITOR = 12000; // 방문자 1명당 추정 객단가 (원)
    private static final String DEFAULT_BEST_MENU = "아메리카노"; // 임시 인기 메뉴

    private final VisionDataRepository visionDataRepository;

    // 지금 몇 명 있나 - 가장 최근 스냅샷에서 아직 나가지 않은(outAt == null) 사람 수
    @Transactional(readOnly = true)
    public CountResponseDto getCurrentCount() {
        final int count = visionDataRepository.findTopByOrderByCapturedAtDesc()
                .map(visionData -> (int) visionData.getPeople().stream()
                        .filter(person -> person.getOutAt() == null)
                        .count())
                .orElse(0);

        return new CountResponseDto(count);
    }

    // 몇 시가 가장 바쁜가 - 구간 내 스냅샷들의 peakTime 중 가장 자주 나타난 시각 (null 제외)
    @Transactional(readOnly = true)
    public TimeResponseDto getPeakTime(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final Map<Integer, Long> peakHourCounts = new HashMap<>();
        for (VisionData visionData : findOverlapping(startAt, endAt)) {
            if (visionData.getPeakTime() == null) {
                continue;
            }
            peakHourCounts.merge(visionData.getPeakTime(), 1L, Long::sum);
        }

        final int time = peakHourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        return new TimeResponseDto(time);
    }

    // 오늘 매출 얼마인가 - POS 연동 전까지 방문자 수(null 제외) 기반 추정 매출
    @Transactional(readOnly = true)
    public DailySalesResponseDto getDailySales(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final int visitors = findOverlapping(startAt, endAt).stream()
                .filter(visionData -> visionData.getTotalCount() != null)
                .mapToInt(VisionData::getTotalCount)
                .sum();

        return new DailySalesResponseDto(visitors * ESTIMATED_SPEND_PER_VISITOR);
    }

    // 어떤 메뉴가 잘 팔리나 - POS 연동 전까지 임시 값
    @Transactional(readOnly = true)
    public MenuResponseDto getBestMenu(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        return new MenuResponseDto(DEFAULT_BEST_MENU);
    }

    // 최대 응대 대기 시간 - 구간 내 maxResponseWaitTime(null 제외)의 최댓값
    @Transactional(readOnly = true)
    public TimeResponseDto getResponseWaitTime(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final int time = findOverlapping(startAt, endAt).stream()
                .filter(visionData -> visionData.getMaxResponseWaitTime() != null)
                .mapToInt(VisionData::getMaxResponseWaitTime)
                .max()
                .orElse(0);

        return new TimeResponseDto(time);
    }

    // 그냥 나간 손님 수 - 구간 내 justLeftCount(null 제외) 합계
    @Transactional(readOnly = true)
    public CountResponseDto getJustLeftCount(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final int count = findOverlapping(startAt, endAt).stream()
                .filter(visionData -> visionData.getJustLeftCount() != null)
                .mapToInt(VisionData::getJustLeftCount)
                .sum();

        return new CountResponseDto(count);
    }

    // 최대 테이블 유휴 시간 - 구간 내 maxEmptyTableTime(null 제외)의 최댓값
    @Transactional(readOnly = true)
    public TimeResponseDto getEmptyTableTime(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final int time = findOverlapping(startAt, endAt).stream()
                .filter(visionData -> visionData.getMaxEmptyTableTime() != null)
                .mapToInt(VisionData::getMaxEmptyTableTime)
                .max()
                .orElse(0);

        return new TimeResponseDto(time);
    }

    // 평균과 비교해서 오늘 얼마나 왔는지 - 구간 합계(null 제외) vs 전체 일평균(null 제외)
    @Transactional(readOnly = true)
    public DailyCountResponseDto getDailyCount(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final int count = findOverlapping(startAt, endAt).stream()
                .filter(visionData -> visionData.getTotalCount() != null)
                .mapToInt(VisionData::getTotalCount)
                .sum();

        // 전체 데이터를 일자별로 합산한 뒤 일평균 계산 (capturedAt 또는 totalCount 가 null 인 스냅샷은 제외)
        final Map<LocalDate, Integer> dailyTotals = new HashMap<>();
        for (VisionData visionData : visionDataRepository.findAll()) {
            if (visionData.getCapturedAt() == null || visionData.getTotalCount() == null) {
                continue;
            }
            dailyTotals.merge(visionData.getCapturedAt().toLocalDate(), visionData.getTotalCount(), Integer::sum);
        }

        final int avgCount = dailyTotals.isEmpty()
                ? 0
                : (int) Math.round(dailyTotals.values().stream().mapToInt(Integer::intValue).average().orElse(0));

        return new DailyCountResponseDto(count, avgCount);
    }

    // 매장 방문 추세 - 구간 내 스냅샷을 시간순으로 (capturedAt, totalCount) 나열 (totalCount 가 null 인 스냅샷은 제외)
    @Transactional(readOnly = true)
    public VisitTrendResponseDto getVisitTrend(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final List<VisionData> inRange = new ArrayList<>(findOverlapping(startAt, endAt));
        inRange.sort(Comparator.comparing(VisionData::getCapturedAt));

        final List<LocalDateTime> time = new ArrayList<>();
        final List<Integer> data = new ArrayList<>();
        for (VisionData visionData : inRange) {
            if (visionData.getTotalCount() == null) {
                continue;
            }
            time.add(visionData.getCapturedAt());
            data.add(visionData.getTotalCount());
        }

        return new VisitTrendResponseDto(time, data);
    }

    // 핵심 고객 - 구간 내 방문자들을 (나이대, 성별) 로 묶어 가장 많은 그룹 (age/gender 가 null 인 사람은 제외)
    @Transactional(readOnly = true)
    public CoreCustomerV2ResponseDto getCoreCustomer(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final List<VisionData> inRange = findOverlapping(startAt, endAt);

        final Map<List<Integer>, Long> buckets = new HashMap<>();
        for (VisionData visionData : inRange) {
            for (VisionPerson person : visionData.getPeople()) {
                if (person.getAge() == null || person.getGender() == null) {
                    continue;
                }
                buckets.merge(List.of(person.getAge(), person.getGender()), 1L, Long::sum);
            }
        }

        if (!buckets.isEmpty()) {
            final List<Integer> top = buckets.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseThrow();
            return new CoreCustomerV2ResponseDto(top.get(0), top.get(1));
        }

        // 개별 방문자 정보가 없으면 스냅샷의 집계된 핵심 고객 필드(null 제외 최신값)로 대체
        return inRange.stream()
                .filter(visionData -> visionData.getCoreCustomerAge() != null && visionData.getCoreCustomerGender() != null)
                .max(Comparator.comparing(VisionData::getCapturedAt))
                .map(visionData -> new CoreCustomerV2ResponseDto(visionData.getCoreCustomerAge(), visionData.getCoreCustomerGender()))
                .orElse(new CoreCustomerV2ResponseDto(0, 0));
    }

    // 평균 체류시간 - 구간 내 스냅샷 avgDwellTime(분, null 제외)의 평균
    @Transactional(readOnly = true)
    public TimeResponseDto getAvgDwell(final LocalDateTime startAt, final LocalDateTime endAt) {
        validateRange(startAt, endAt);

        final double avgMinutes = findOverlapping(startAt, endAt).stream()
                .filter(visionData -> visionData.getAvgDwellTime() != null)
                .mapToInt(VisionData::getAvgDwellTime)
                .average()
                .orElse(0);

        return new TimeResponseDto((int) Math.round(avgMinutes));
    }

    private List<VisionData> findOverlapping(final LocalDateTime startAt, final LocalDateTime endAt) {
        return visionDataRepository.findOverlapping(startAt, endAt);
    }

    private void validateRange(final LocalDateTime startAt, final LocalDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }
    }
}
