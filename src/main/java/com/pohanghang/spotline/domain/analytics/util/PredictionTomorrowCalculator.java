package com.pohanghang.spotline.domain.analytics.util;

import com.pohanghang.spotline.domain.analytics.dto.PredictionTomorrowResponseDto;
import com.pohanghang.spotline.domain.analytics.entity.Weather;
import com.pohanghang.spotline.domain.analytics.repository.AnalyticsRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

public class PredictionTomorrowCalculator {

    public static PredictionTomorrowResponseDto calculate(
            final List<AnalyticsRepository.WeatherImpactRow> rows,
            final Weather tomorrowWeather,
            final LocalDate targetDate
    ) {
        DayOfWeek targetDayOfWeek = targetDate.getDayOfWeek();

        // 1. Group data by Date
        Map<LocalDate, DailyStats> dailyData = new HashMap<>();
        for (AnalyticsRepository.WeatherImpactRow row : rows) {
            if (row.getStartAt() == null) continue;
            LocalDate date = row.getStartAt().toLocalDate();
            dailyData.putIfAbsent(date, new DailyStats(date, row.getWeather()));
            dailyData.get(date).addVisits(row.getTotalCount() != null ? row.getTotalCount() : 0);
        }

        List<LocalDate> sortedDates = new ArrayList<>(dailyData.keySet());
        Collections.sort(sortedDates);
        
        // 2. Base value (같은 요일 최근 4주 평균 방문 수)
        List<Integer> sameDayVisits = new ArrayList<>();
        int count4Weeks = 0;
        double sum4Weeks = 0;
        
        // Find recent 4 occurrences of targetDayOfWeek before tomorrow
        for (int i = sortedDates.size() - 1; i >= 0; i--) {
            LocalDate d = sortedDates.get(i);
            if (d.getDayOfWeek() == targetDayOfWeek && d.isBefore(targetDate)) {
                sameDayVisits.add(dailyData.get(d).getTotalVisits());
                if (count4Weeks < 4) {
                    sum4Weeks += dailyData.get(d).getTotalVisits();
                    count4Weeks++;
                }
            }
        }
        
        double baseValue = count4Weeks > 0 ? sum4Weeks / count4Weeks : 0.0;
        if (baseValue == 0.0) {
            // fallback: use overall average
            double overallAvg = dailyData.values().stream().mapToInt(v -> v.getTotalVisits()).average().orElse(0);
            baseValue = overallAvg;
        }

        // 3. Weather adjustment (과거 유사 날씨 날들의 평균 방문 / 전체 평균 방문)
        double totalAvgVisits = dailyData.values().stream().mapToInt(v -> v.getTotalVisits()).average().orElse(0);
        
        List<Integer> similarWeatherVisits = dailyData.values().stream()
                .filter(stats -> stats.weather == tomorrowWeather)
                .map(stats -> stats.getTotalVisits())
                .toList();
        
        double weatherAvgVisits = similarWeatherVisits.stream().mapToInt(Integer::intValue).average().orElse(totalAvgVisits);
        
        double weatherCoefficient = totalAvgVisits > 0 ? weatherAvgVisits / totalAvgVisits : 1.0;
        weatherCoefficient = Math.max(0.5, Math.min(1.5, weatherCoefficient));

        // 4. Trend reflection (MA5 / MA20)
        double ma5 = getMovingAverage(sortedDates, dailyData, 5);
        double ma20 = getMovingAverage(sortedDates, dailyData, 20);
        
        double trendCoefficient = 1.0;
        if (ma20 > 0) {
            trendCoefficient = ma5 / ma20;
            trendCoefficient = Math.max(0.5, Math.min(1.5, trendCoefficient));
        }

        // 5. Final Prediction
        double prediction = baseValue * weatherCoefficient * trendCoefficient;
        
        // Standard Deviation of same day of week
        double stdDev = calculateStdDev(sameDayVisits);
        if (stdDev == 0.0 && !sameDayVisits.isEmpty()) {
            stdDev = prediction * 0.1; // Default fallback to 10% variance
        } else if (stdDev == 0.0) {
            stdDev = Math.max(10.0, prediction * 0.1);
        }

        int expectedVisits = (int) Math.round(prediction);
        int minRange = (int) Math.max(0, Math.round(prediction - 1.5 * stdDev));
        int maxRange = (int) Math.round(prediction + 1.5 * stdDev);

        return new PredictionTomorrowResponseDto(
                expectedVisits,
                minRange,
                maxRange
        );
    }

    private static double getMovingAverage(List<LocalDate> sortedDates, Map<LocalDate, DailyStats> dailyData, int days) {
        if (sortedDates.isEmpty()) return 0.0;
        int count = 0;
        double sum = 0;
        for (int i = sortedDates.size() - 1; i >= Math.max(0, sortedDates.size() - days); i--) {
            sum += dailyData.get(sortedDates.get(i)).getTotalVisits();
            count++;
        }
        return count > 0 ? sum / count : 0.0;
    }
    
    private static double calculateStdDev(List<Integer> values) {
        if (values == null || values.size() <= 1) return 0.0;
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / values.size();
        return Math.sqrt(variance);
    }

    private static class DailyStats {
        LocalDate date;
        Weather weather;
        private int sumVisits = 0;
        private int recordCount = 0;

        public DailyStats(LocalDate date, Weather weather) {
            this.date = date;
            this.weather = weather;
        }

        public void addVisits(int visits) {
            this.sumVisits += visits;
            this.recordCount++;
        }

        public int getTotalVisits() {
            return recordCount > 0 ? (int) Math.round((double) sumVisits / recordCount) : 0;
        }
    }
}
