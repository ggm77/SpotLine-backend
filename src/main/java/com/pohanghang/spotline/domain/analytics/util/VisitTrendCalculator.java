package com.pohanghang.spotline.domain.analytics.util;

import com.pohanghang.spotline.domain.analytics.dto.PerformanceResultResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.VisitCountResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.WeatherImpactRequestDto;
import com.pohanghang.spotline.domain.analytics.model.AnalyticsRow;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public final class VisitTrendCalculator {

    private VisitTrendCalculator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static VisitCountResponseDto calculateTrend(
            final LocalDateTime startAt,
            final LocalDateTime endAt,
            final List<AnalyticsRow> rows
    ) {
        final SortedSet<LocalDate> allDates = new TreeSet<>();
        for (AnalyticsRow row : rows) {
            if (row.getStartAt() != null) {
                allDates.add(row.getStartAt().toLocalDate());
            }
        }

        final Map<LocalDate, PerformanceResultResponseDto> performanceMap = new HashMap<>();
        for (LocalDate date : allDates) {
            try {
                final WeatherImpactRequestDto requestDto = new WeatherImpactRequestDto(date.atStartOfDay());
                final PerformanceResultResponseDto result = WeatherImpactCalculator.calculate(requestDto, rows);
                performanceMap.put(date, result);
            } catch (Exception e) {
                // Skip days that lack weather data or cannot be predicted
            }
        }

        final List<LocalDate> validDates = new ArrayList<>();
        for (LocalDate date : allDates) {
            if (performanceMap.containsKey(date)) {
                validDates.add(date);
            }
        }

        final List<Float> adjustedValues = new ArrayList<>();
        for (LocalDate date : validDates) {
            adjustedValues.add(performanceMap.get(date).adjustedValue());
        }

        final List<String> dateStrings = new ArrayList<>();
        final List<Integer> actualList = new ArrayList<>();
        final List<Integer> adjustedList = new ArrayList<>();
        final List<Integer> ma5List = new ArrayList<>();
        final List<Integer> ma10List = new ArrayList<>();
        final List<Integer> ma20List = new ArrayList<>();
        final List<Integer> ma60List = new ArrayList<>();

        Integer prevMa5 = null;
        Integer prevMa20 = null;
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        for (int i = 0; i < validDates.size(); i++) {
            final LocalDate date = validDates.get(i);
            final PerformanceResultResponseDto perf = performanceMap.get(date);

            final Integer actual = Math.round(perf.realValue());
            final Integer adjusted = Math.round(perf.adjustedValue());

            final Integer ma5 = calculateMA(adjustedValues, i, 5);
            final Integer ma10 = calculateMA(adjustedValues, i, 10);
            final Integer ma20 = calculateMA(adjustedValues, i, 20);
            final Integer ma60 = calculateMA(adjustedValues, i, 60);

            if (ma5 != null && ma20 != null && prevMa5 != null && prevMa20 != null) {
                if (ma5 > ma20 && prevMa5 <= prevMa20) {
                    log.info("🟡 단기 상승 추세 시작");
                    log.info("📈 지난 2주 순수 방문 추세 상승 중");
                } else if (ma5 < ma20 && prevMa5 >= prevMa20) {
                    log.info("🔴 단기 하락 추세 시작, 주의 필요");
                }
            }

            prevMa5 = ma5;
            prevMa20 = ma20;

            final boolean inRange = !date.isBefore(startAt.toLocalDate()) && !date.isAfter(endAt.toLocalDate());
            if (inRange) {
                dateStrings.add(date.atStartOfDay().format(formatter));
                actualList.add(actual);
                adjustedList.add(adjusted);
                ma5List.add(ma5);
                ma10List.add(ma10);
                ma20List.add(ma20);
                ma60List.add(ma60);
            }
        }

        final List<List<Integer>> data = Arrays.asList(
                actualList,
                adjustedList,
                ma5List,
                ma10List,
                ma20List,
                ma60List
        );

        return new VisitCountResponseDto(dateStrings, data);
    }

    private static Integer calculateMA(final List<Float> values, final int currentIndex, final int period) {
        if (currentIndex < period - 1) {
            return null;
        }
        float sum = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += values.get(i);
        }
        return Math.round(sum / period);
    }
}
