package com.pohanghang.spotline.domain.analytics.util;

import com.pohanghang.spotline.domain.analytics.dto.PerformanceResultResponseDto;
import com.pohanghang.spotline.domain.analytics.dto.WeatherImpactRequestDto;
import com.pohanghang.spotline.domain.analytics.entity.Weather;
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

public final class WeatherImpactCalculator {

    private static final int WEATHER_MODEL_FEATURE_COUNT = 9;
    private static final double REGULARIZATION = 1e-6;

    private WeatherImpactCalculator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static PerformanceResultResponseDto calculate(
            final WeatherImpactRequestDto weatherImpactRequestDto,
            final List<AnalyticsRepository.WeatherImpactRow> weatherImpactRows
    ) {
        if (weatherImpactRows.isEmpty()) {
            throw new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND);
        }

        final LocalDate targetDate = weatherImpactRequestDto.day().toLocalDate();
        final Map<LocalDate, DailyWeatherVisit> dailyWeatherVisits = toDailyWeatherVisits(weatherImpactRows);
        final DailyWeatherVisit targetDailyWeatherVisit = dailyWeatherVisits.get(targetDate);
        if (targetDailyWeatherVisit == null) {
            throw new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND);
        }

        final List<WeatherImpactObservation> observations = toWeatherImpactObservations(
                dailyWeatherVisits,
                targetDate,
                weatherImpactRequestDto
        );
        if (observations.isEmpty()) {
            throw new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND);
        }

        final double targetRain = resolveRainValue(targetDailyWeatherVisit, weatherImpactRequestDto);
        final double targetTemperature = resolveTemperature(targetDailyWeatherVisit, weatherImpactRequestDto);
        final double[] coefficients = estimateWeatherImpactCoefficients(observations);
        final double expectedValue = normalizeExpectedValue(
                predict(coefficients, targetRain, targetTemperature, targetDate.getDayOfWeek())
        );
        final double realValue = targetDailyWeatherVisit.totalCount();

        return new PerformanceResultResponseDto(
                (float) realValue,
                (float) expectedValue,
                toPerformanceResult(realValue / expectedValue)
        );
    }

    private static Map<LocalDate, DailyWeatherVisit> toDailyWeatherVisits(
            final List<AnalyticsRepository.WeatherImpactRow> weatherImpactRows
    ) {
        final Map<LocalDate, DailyWeatherVisit> dailyWeatherVisits = new HashMap<>();
        for (AnalyticsRepository.WeatherImpactRow weatherImpactRow : weatherImpactRows) {
            if (weatherImpactRow.getStartAt() == null || weatherImpactRow.getTotalCount() == null) {
                continue;
            }

            final LocalDate date = weatherImpactRow.getStartAt().toLocalDate();
            dailyWeatherVisits.computeIfAbsent(date, DailyWeatherVisit::new)
                    .add(
                            weatherImpactRow.getTotalCount(),
                            weatherImpactRow.getWeather(),
                            weatherImpactRow.getTemperature()
                    );
        }

        return dailyWeatherVisits;
    }

    private static List<WeatherImpactObservation> toWeatherImpactObservations(
            final Map<LocalDate, DailyWeatherVisit> dailyWeatherVisits,
            final LocalDate targetDate,
            final WeatherImpactRequestDto weatherImpactRequestDto
    ) {
        final List<WeatherImpactObservation> observations = new ArrayList<>();
        for (DailyWeatherVisit dailyWeatherVisit : dailyWeatherVisits.values()) {
            Double temperature = dailyWeatherVisit.averageTemperature();
            if (temperature == null && dailyWeatherVisit.date().equals(targetDate) && weatherImpactRequestDto.temp() != null) {
                temperature = weatherImpactRequestDto.temp().doubleValue();
            }

            Integer rain = dailyWeatherVisit.rain();
            if (rain == null && dailyWeatherVisit.date().equals(targetDate) && weatherImpactRequestDto.rain() != null) {
                rain = toRainValue(weatherImpactRequestDto.rain());
            }

            if (temperature == null || rain == null) {
                continue;
            }

            observations.add(new WeatherImpactObservation(
                    dailyWeatherVisit.totalCount(),
                    rain,
                    temperature,
                    dailyWeatherVisit.date().getDayOfWeek()
            ));
        }

        return observations;
    }

    private static double resolveRainValue(
            final DailyWeatherVisit targetDailyWeatherVisit,
            final WeatherImpactRequestDto weatherImpactRequestDto
    ) {
        if (targetDailyWeatherVisit.rain() != null) {
            return targetDailyWeatherVisit.rain();
        }

        if (weatherImpactRequestDto.rain() != null) {
            return toRainValue(weatherImpactRequestDto.rain());
        }

        throw new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND);
    }

    private static double normalizeExpectedValue(final double expectedValue) {
        if (Double.isNaN(expectedValue) || Double.isInfinite(expectedValue)) {
            return 1.0;
        }

        return Math.max(1.0, expectedValue);
    }

    private static double resolveTemperature(
            final DailyWeatherVisit targetDailyWeatherVisit,
            final WeatherImpactRequestDto weatherImpactRequestDto
    ) {
        final Double averageTemperature = targetDailyWeatherVisit.averageTemperature();
        if (averageTemperature != null) {
            return averageTemperature;
        }

        if (weatherImpactRequestDto.temp() != null) {
            return weatherImpactRequestDto.temp();
        }

        throw new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND);
    }

    private static int toRainValue(final Integer rain) {
        if (rain > 0) {
            return 1;
        }

        return 0;
    }

    private static double[] estimateWeatherImpactCoefficients(final List<WeatherImpactObservation> observations) {
        final double[][] xtx = new double[WEATHER_MODEL_FEATURE_COUNT][WEATHER_MODEL_FEATURE_COUNT];
        final double[] xty = new double[WEATHER_MODEL_FEATURE_COUNT];

        for (WeatherImpactObservation observation : observations) {
            final double[] features = toWeatherImpactFeatures(
                    observation.rain(),
                    observation.temperature(),
                    observation.dayOfWeek()
            );

            for (int row = 0; row < WEATHER_MODEL_FEATURE_COUNT; row++) {
                xty[row] += features[row] * observation.totalCount();
                for (int column = 0; column < WEATHER_MODEL_FEATURE_COUNT; column++) {
                    xtx[row][column] += features[row] * features[column];
                }
            }
        }

        for (int index = 1; index < WEATHER_MODEL_FEATURE_COUNT; index++) {
            xtx[index][index] += REGULARIZATION;
        }

        return solveLinearSystem(xtx, xty);
    }

    private static double predict(
            final double[] coefficients,
            final double rain,
            final double temperature,
            final DayOfWeek dayOfWeek
    ) {
        final double[] features = toWeatherImpactFeatures(rain, temperature, dayOfWeek);
        double prediction = 0.0;
        for (int index = 0; index < WEATHER_MODEL_FEATURE_COUNT; index++) {
            prediction += coefficients[index] * features[index];
        }

        return prediction;
    }

    private static double[] toWeatherImpactFeatures(
            final double rain,
            final double temperature,
            final DayOfWeek dayOfWeek
    ) {
        final double[] features = new double[WEATHER_MODEL_FEATURE_COUNT];
        features[0] = 1.0;
        features[1] = rain;
        features[2] = temperature;

        final int dayOfWeekValue = dayOfWeek.getValue();
        if (dayOfWeekValue < DayOfWeek.SUNDAY.getValue()) {
            features[2 + dayOfWeekValue] = 1.0;
        }

        return features;
    }

    private static double[] solveLinearSystem(
            final double[][] coefficients,
            final double[] constants
    ) {
        final int size = constants.length;
        final double[][] matrix = new double[size][size + 1];
        for (int row = 0; row < size; row++) {
            System.arraycopy(coefficients[row], 0, matrix[row], 0, size);
            matrix[row][size] = constants[row];
        }

        for (int pivot = 0; pivot < size; pivot++) {
            int maxRow = pivot;
            for (int row = pivot + 1; row < size; row++) {
                if (Math.abs(matrix[row][pivot]) > Math.abs(matrix[maxRow][pivot])) {
                    maxRow = row;
                }
            }

            final double[] temp = matrix[pivot];
            matrix[pivot] = matrix[maxRow];
            matrix[maxRow] = temp;

            if (Math.abs(matrix[pivot][pivot]) < REGULARIZATION) {
                matrix[pivot][pivot] = REGULARIZATION;
            }

            for (int row = pivot + 1; row < size; row++) {
                final double factor = matrix[row][pivot] / matrix[pivot][pivot];
                for (int column = pivot; column <= size; column++) {
                    matrix[row][column] -= factor * matrix[pivot][column];
                }
            }
        }

        final double[] solution = new double[size];
        for (int row = size - 1; row >= 0; row--) {
            double sum = matrix[row][size];
            for (int column = row + 1; column < size; column++) {
                sum -= matrix[row][column] * solution[column];
            }
            solution[row] = sum / matrix[row][row];
        }

        return solution;
    }

    private static PerformanceResult toPerformanceResult(final double performanceRatio) {
        if (performanceRatio >= 1.05) {
            return PerformanceResult.GOOD;
        }

        if (performanceRatio >= 0.9) {
            return PerformanceResult.NORMAL;
        }

        return PerformanceResult.BAD;
    }

    private static class DailyWeatherVisit {
        private final LocalDate date;
        private int totalCount;
        private int rain;
        private boolean hasWeather;
        private double temperatureSum;
        private int temperatureCount;

        private DailyWeatherVisit(final LocalDate date) {
            this.date = date;
        }

        private void add(
                final int totalCount,
                final Weather weather,
                final Double temperature
        ) {
            this.totalCount += totalCount;
            if (weather != null) {
                this.hasWeather = true;
                this.rain = Math.max(this.rain, toRainValue(weather));
            }
            if (temperature != null) {
                this.temperatureSum += temperature;
                this.temperatureCount++;
            }
        }

        private LocalDate date() {
            return date;
        }

        private int totalCount() {
            return totalCount;
        }

        private Integer rain() {
            if (!hasWeather) {
                return null;
            }

            return rain;
        }

        private Double averageTemperature() {
            if (temperatureCount == 0) {
                return null;
            }

            return temperatureSum / temperatureCount;
        }

        private static int toRainValue(final Weather weather) {
            if (weather == Weather.RAINY || weather == Weather.SNOW) {
                return 1;
            }

            return 0;
        }
    }

    private record WeatherImpactObservation(
            double totalCount,
            double rain,
            double temperature,
            DayOfWeek dayOfWeek
    ) {
    }
}
