package com.pohanghang.spotline.domain.analytics.util;

import com.pohanghang.spotline.domain.analytics.dto.PerformanceResultResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.WeekdayPatternRequestDto;
import com.pohanghang.spotline.domain.analytics.repository.AnalyticsRepository;
import com.pohanghang.spotline.domain.video.entity.PerformanceResult;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WeekdayPatternCalculator {

    private WeekdayPatternCalculator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static PerformanceResultResponseDto calculate(
            final WeekdayPatternRequestDto requestDto,
            final List<AnalyticsRepository.WeatherImpactRow> rows
    ) {
        if (rows.isEmpty()) {
            throw new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND);
        }

        final LocalDate targetDate = requestDto.day().toLocalDate();
        final DayOfWeek targetDayOfWeek = targetDate.getDayOfWeek();

        final Map<LocalDate, int[]> dailyVisitStats = new HashMap<>();
        for (AnalyticsRepository.WeatherImpactRow row : rows) {
            if (row.getStartAt() == null || row.getTotalCount() == null) {
                continue;
            }
            final LocalDate date = row.getStartAt().toLocalDate();
            int[] stats = dailyVisitStats.computeIfAbsent(date, k -> new int[2]);
            stats[0] += row.getTotalCount();
            stats[1]++;
        }

        final Map<LocalDate, Integer> dailyVisits = new HashMap<>();
        for (Map.Entry<LocalDate, int[]> entry : dailyVisitStats.entrySet()) {
            int[] stats = entry.getValue();
            dailyVisits.put(entry.getKey(), (int) Math.round((double) stats[0] / stats[1]));
        }

        if (!dailyVisits.containsKey(targetDate)) {
            throw new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND);
        }

        final int targetValue = dailyVisits.get(targetDate);

        final List<Integer> historicalVisits = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : dailyVisits.entrySet()) {
            if (entry.getKey().isBefore(targetDate) && entry.getKey().getDayOfWeek() == targetDayOfWeek) {
                historicalVisits.add(entry.getValue());
            }
        }

        if (historicalVisits.isEmpty()) {
            return new PerformanceResultResponseDto(
                    (float) targetValue,
                    (float) targetValue,
                    0f,
                    PerformanceResult.NORMAL
            );
        }

        double sum = 0;
        for (int count : historicalVisits) {
            sum += count;
        }
        final double mean = sum / historicalVisits.size();

        double sumSq = 0;
        for (int count : historicalVisits) {
            sumSq += Math.pow(count - mean, 2);
        }
        final double stdDev = Math.sqrt(sumSq / historicalVisits.size());

        double zScore = 0;
        if (stdDev > 0) {
            zScore = (targetValue - mean) / stdDev;
        } else if (targetValue != mean) {
            zScore = targetValue > mean ? 2.0 : -2.0;
        }

        PerformanceResult result;
        if (zScore <= -2.0) {
            result = PerformanceResult.BAD;
        } else if (zScore >= 2.0) {
            result = PerformanceResult.GOOD;
        } else {
            result = PerformanceResult.NORMAL;
        }

        return new PerformanceResultResponseDto(
                (float) targetValue,
                (float) mean,
                (float) zScore,
                result
        );
    }
}
