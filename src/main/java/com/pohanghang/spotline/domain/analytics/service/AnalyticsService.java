package com.pohanghang.spotline.domain.analytics.service;

import com.pohanghang.spotline.domain.analytics.dto.*;
import com.pohanghang.spotline.domain.analytics.entity.AgeGroup;
import com.pohanghang.spotline.domain.analytics.entity.Analytics;
import com.pohanghang.spotline.domain.analytics.repository.AnalyticsRepository;
import com.pohanghang.spotline.domain.analytics.util.WeatherImpactCalculator;
import com.pohanghang.spotline.domain.analytics.util.WeekdayPatternCalculator;
import com.pohanghang.spotline.domain.analytics.util.VisitTrendCalculator;
import com.pohanghang.spotline.domain.analytics.util.PredictionTomorrowCalculator;
import com.pohanghang.spotline.domain.analytics.entity.Weather;
import com.pohanghang.spotline.global.infra.openmeteo.OpenMeteoClient;
import com.pohanghang.spotline.domain.video.entity.Video;
import com.pohanghang.spotline.domain.video.entity.PerformanceResult;
import com.pohanghang.spotline.domain.video.repository.VideoRepository;
import com.pohanghang.spotline.global.infra.gemini.GeminiClient;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Map<AgeGroup, String> AGE_GROUP_LABELS = Map.of(
            AgeGroup.CHILD, "00s",
            AgeGroup.TEN, "10s",
            AgeGroup.TWENTY, "20s",
            AgeGroup.THIRTY, "30s",
            AgeGroup.FORTY, "40s",
            AgeGroup.FIFTY_PLUS, "50s+",
            AgeGroup.UNKNOWN, "UNKNOWN"
    );

    private final AnalyticsRepository analyticsRepository;
    private final VideoRepository videoRepository;
    private final OpenMeteoClient openMeteoClient;
    private final GeminiClient geminiClient;

    public RawAnalyticsDto getRawAnalytics(final Long videoId) {
        // 1) null 검사
        if (videoId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) video 조회
        final Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new CustomException(ExceptionCode.VIDEO_NOT_FOUND));

        // 3) video로 analytics 조회
        final Analytics analytics = analyticsRepository.findByVideo(video)
                .orElseThrow(() -> new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND));

        // 4) 문자열 파싱
        return JsonUtil.toObject(analytics.getRawData(), RawAnalyticsDto.class);
    }

    @Transactional(readOnly = true)
    public CoreCustomerResponseDto getCoreCustomers(final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {
        // 1) null 검사
        if (defaultStartAtEndAtRequestDto == null
                || defaultStartAtEndAtRequestDto.startAt() == null
                || defaultStartAtEndAtRequestDto.endAt() == null
                || !defaultStartAtEndAtRequestDto.startAt().isBefore(defaultStartAtEndAtRequestDto.endAt())) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final LocalDateTime startAt = defaultStartAtEndAtRequestDto.startAt();
        final LocalDateTime endAt = defaultStartAtEndAtRequestDto.endAt();

        final List<AnalyticsRepository.CoreCustomerGroup> coreCustomerGroups =
                analyticsRepository.findCoreCustomerGroups(startAt, endAt);

        return coreCustomerGroups.stream()
                .findFirst()
                .map(coreCustomerGroup -> new CoreCustomerResponseDto(
                        coreCustomerGroup.getGender().name(),
                        AGE_GROUP_LABELS.get(coreCustomerGroup.getAgeGroup())
                ))
                .orElseThrow(() -> new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public AgeGroupDistributionDto getHourlyPopulation(final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {
        // 1) null 검사
        if (defaultStartAtEndAtRequestDto == null
                || defaultStartAtEndAtRequestDto.startAt() == null
                || defaultStartAtEndAtRequestDto.endAt() == null
                || !defaultStartAtEndAtRequestDto.startAt().isBefore(defaultStartAtEndAtRequestDto.endAt())) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final LocalDateTime startAt = defaultStartAtEndAtRequestDto.startAt();
        final LocalDateTime endAt = defaultStartAtEndAtRequestDto.endAt();

        final Map<AgeGroup, Integer> ageGroupCounts = new EnumMap<>(AgeGroup.class);
        for (AgeGroup ageGroup : AgeGroup.values()) {
            ageGroupCounts.put(ageGroup, 0);
        }

        final List<AnalyticsRepository.HourlyPopulationGroup> hourlyPopulationGroups =
                analyticsRepository.findHourlyPopulationGroups(startAt, endAt);
        for (AnalyticsRepository.HourlyPopulationGroup hourlyPopulationGroup : hourlyPopulationGroups) {
            ageGroupCounts.put(
                    hourlyPopulationGroup.getAgeGroup(),
                    Math.toIntExact(hourlyPopulationGroup.getTotalCount())
            );
        }

        return new AgeGroupDistributionDto(
                ageGroupCounts.get(AgeGroup.CHILD),
                ageGroupCounts.get(AgeGroup.TEN),
                ageGroupCounts.get(AgeGroup.TWENTY),
                ageGroupCounts.get(AgeGroup.THIRTY),
                ageGroupCounts.get(AgeGroup.FORTY),
                ageGroupCounts.get(AgeGroup.FIFTY_PLUS)
        );
    }

    @Transactional(readOnly = true)
    public PerformanceResultResponseDto getWeatherImpact(final WeatherImpactRequestDto weatherImpactRequestDto) {
        if (weatherImpactRequestDto == null || weatherImpactRequestDto.day() == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final List<AnalyticsRepository.WeatherImpactRow> weatherImpactRows = analyticsRepository.findWeatherImpactRows();
        return WeatherImpactCalculator.calculate(weatherImpactRequestDto, weatherImpactRows);
    }
    
    @Transactional(readOnly = true)
    public PerformanceResultResponseDto getWeekdayPatterns(final WeekdayPatternRequestDto weekdayPatternRequestDto) {
        if (weekdayPatternRequestDto == null || weekdayPatternRequestDto.day() == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final List<AnalyticsRepository.WeatherImpactRow> rows = analyticsRepository.findWeatherImpactRows();
        return WeekdayPatternCalculator.calculate(weekdayPatternRequestDto, rows);
    }

    @Transactional(readOnly = true)
    public VisitCountResponseDto getVisitCount(final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {
        if (defaultStartAtEndAtRequestDto == null
                || defaultStartAtEndAtRequestDto.startAt() == null
                || defaultStartAtEndAtRequestDto.endAt() == null
                || !defaultStartAtEndAtRequestDto.startAt().isBefore(defaultStartAtEndAtRequestDto.endAt())) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final List<AnalyticsRepository.WeatherImpactRow> rows = analyticsRepository.findWeatherImpactRows();
        return VisitTrendCalculator.calculateTrend(
                defaultStartAtEndAtRequestDto.startAt(),
                defaultStartAtEndAtRequestDto.endAt(),
                rows
        );
    }

    @Transactional(readOnly = true)
    public PredictionTomorrowResponseDto getPredictionTomorrow() {
        final List<AnalyticsRepository.WeatherImpactRow> rows = analyticsRepository.findWeatherImpactRows();

        LocalDateTime tomorrowAfternoon = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .plusDays(1)
                .withHour(14)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        OpenMeteoClient.WeatherData weatherData = openMeteoClient.getSeoulWeatherData(tomorrowAfternoon);
        Weather tomorrowWeather = weatherData.weather();

        return PredictionTomorrowCalculator.calculate(rows, tomorrowWeather, tomorrowAfternoon.toLocalDate());
    }

    @Transactional(readOnly = true)
    public PredictionNextWeekResponseDto getPredictionNextWeek() {
        final List<AnalyticsRepository.WeatherImpactRow> rows = analyticsRepository.findWeatherImpactRows();
        List<PredictionTomorrowResponseDto> nextWeekPredictions = new java.util.ArrayList<>();

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        
        for (int i = 1; i <= 7; i++) {
            LocalDateTime targetAfternoon = now.plusDays(i)
                    .withHour(14)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            OpenMeteoClient.WeatherData weatherData = openMeteoClient.getSeoulWeatherData(targetAfternoon);
            Weather targetWeather = weatherData.weather();

            PredictionTomorrowResponseDto prediction = PredictionTomorrowCalculator.calculate(
                    rows, targetWeather, targetAfternoon.toLocalDate()
            );
            nextWeekPredictions.add(prediction);
        }

        return new PredictionNextWeekResponseDto(nextWeekPredictions);
    }

    @Transactional(readOnly = true)
    public MessageResponseDto getDailyBriefing() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate today = now.toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd = today.atStartOfDay();

        List<AnalyticsRepository.WeatherImpactRow> rows = analyticsRepository.findWeatherImpactRows();

        int yesterdayVisits = 0;
        int lastWeekVisits = 0;
        Weather yesterdayWeather = Weather.SUNNY;

        for (AnalyticsRepository.WeatherImpactRow row : rows) {
            if (row.getStartAt() == null || row.getTotalCount() == null) continue;
            LocalDate rowDate = row.getStartAt().toLocalDate();
            if (rowDate.equals(yesterday)) {
                yesterdayVisits += row.getTotalCount();
                if (row.getWeather() != null) yesterdayWeather = row.getWeather();
            } else if (rowDate.equals(yesterday.minusDays(7))) {
                lastWeekVisits += row.getTotalCount();
            }
        }
        
        int diffPercent = lastWeekVisits > 0 ? (int) Math.round((double)(yesterdayVisits - lastWeekVisits) / lastWeekVisits * 100) : 0;
        String diffSign = diffPercent >= 0 ? "+" : "";

        PerformanceResultResponseDto zScoreResult;
        try {
            zScoreResult = WeekdayPatternCalculator.calculate(new WeekdayPatternRequestDto(yesterdayStart, yesterdayStart.getDayOfWeek().getValue()), rows);
        } catch (Exception e) {
            zScoreResult = new PerformanceResultResponseDto((float) yesterdayVisits, (float) yesterdayVisits, 0f, PerformanceResult.NORMAL);
        }
        String zScoreStr = String.format("%.1f", zScoreResult.adjustedValue());
        String zScoreDesc = zScoreResult.result() == PerformanceResult.GOOD ? "좋음" :
                (zScoreResult.result() == PerformanceResult.BAD ? "나쁨" : "정상");

        List<AnalyticsRepository.CoreCustomerGroup> coreGroups = analyticsRepository.findCoreCustomerGroups(yesterdayStart, yesterdayEnd);
        String coreCustomerStr = "데이터 없음";
        if (!coreGroups.isEmpty()) {
            AnalyticsRepository.CoreCustomerGroup topGroup = coreGroups.get(0);
            String genderStr = "MALE".equals(topGroup.getGender().name()) ? "남성" : ("FEMALE".equals(topGroup.getGender().name()) ? "여성" : "성별미상");
            String ageStr = AGE_GROUP_LABELS.getOrDefault(topGroup.getAgeGroup(), "알수없음");
            int totalYesterdayPersons = coreGroups.stream().mapToInt(g -> g.getTotalCount().intValue()).sum();
            int topPercent = totalYesterdayPersons > 0 ? (int) Math.round((double) topGroup.getTotalCount() / totalYesterdayPersons * 100) : 0;
            coreCustomerStr = String.format("%s %s %d%%", ageStr, genderStr, topPercent);
        }

        List<Analytics> allAnalytics = analyticsRepository.findAll();
        double yesterdayDwellSum = 0;
        int yesterdayDwellCount = 0;
        double overallDwellSum = 0;
        int overallDwellCount = 0;
        
        for (Analytics a : allAnalytics) {
            if (a.getAvgDwellTimeSeconds() != null) {
                overallDwellSum += a.getAvgDwellTimeSeconds();
                overallDwellCount++;
                if (!a.getStartAt().isBefore(yesterdayStart) && a.getStartAt().isBefore(yesterdayEnd)) {
                    yesterdayDwellSum += a.getAvgDwellTimeSeconds();
                    yesterdayDwellCount++;
                }
            }
        }
        
        int yesterdayDwellMins = yesterdayDwellCount > 0 ? (int) Math.round((yesterdayDwellSum / yesterdayDwellCount) / 60.0) : 0;
        int overallDwellMins = overallDwellCount > 0 ? (int) Math.round((overallDwellSum / overallDwellCount) / 60.0) : 0;
        int dwellDiff = yesterdayDwellMins - overallDwellMins;
        String dwellDiffStr = dwellDiff >= 0 ? "+" + dwellDiff : String.valueOf(dwellDiff);

        PerformanceResultResponseDto weatherImpactResult;
        try {
            weatherImpactResult = WeatherImpactCalculator.calculate(new WeatherImpactRequestDto(yesterdayStart), rows);
        } catch (Exception e) {
            weatherImpactResult = new PerformanceResultResponseDto((float) yesterdayVisits, (float) yesterdayVisits, (float) yesterdayVisits, PerformanceResult.NORMAL);
        }
        String weatherResultDesc = weatherImpactResult.result() == PerformanceResult.GOOD ? "선방" :
                (weatherImpactResult.result() == PerformanceResult.BAD ? "부진" : "정상");
        String weatherName = yesterdayWeather == Weather.SUNNY ? "맑음" :
                             (yesterdayWeather == Weather.CLOUDY ? "흐림" :
                              (yesterdayWeather == Weather.RAINY ? "비" : "눈"));

        PredictionTomorrowResponseDto todayPrediction;
        String todayWeatherStr = "알수없음";
        try {
            LocalDateTime todayAfternoon = now.withHour(14).withMinute(0).withSecond(0).withNano(0);
            OpenMeteoClient.WeatherData todayWeather = openMeteoClient.getSeoulWeatherData(todayAfternoon);
            Weather twW = todayWeather.weather();
            todayWeatherStr = twW == Weather.SUNNY ? "맑음" : (twW == Weather.CLOUDY ? "흐림" : (twW == Weather.RAINY ? "비" : "눈"));
            todayPrediction = PredictionTomorrowCalculator.calculate(rows, twW, today);
        } catch (Exception e) {
            todayPrediction = new PredictionTomorrowResponseDto(yesterdayVisits, yesterdayVisits, yesterdayVisits);
        }

        String prompt = String.format(
                "어제 방문 %d명 (%s%d%%, z=%s, %s).\n" +
                "핵심 고객 %s.\n" +
                "평균 체류 %d분 (평소%s분).\n" +
                "날씨 %s, 보정 후 %s.\n" +
                "오늘 예측 %d명, 오후 %s 예보.\n" +
                "위 내용을 일일 브리핑 형식으로 변환해줘.",
                yesterdayVisits, diffSign, diffPercent, zScoreStr, zScoreDesc,
                coreCustomerStr,
                yesterdayDwellMins, dwellDiffStr,
                weatherName, weatherResultDesc,
                todayPrediction.expectedVisits(), todayWeatherStr
        );

        String responseMessage = geminiClient.generateContent(prompt);
        return new MessageResponseDto(responseMessage);
    }
}
