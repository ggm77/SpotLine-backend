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

    private static final String SYSTEM_PROMPT = "지금부터 당신은 마케팅 전문가가 되어 사장님을 위한 마케팅/운영 제안을 전략적이게 제안합니다.\n볼드 표시를 포함한 각종 md파일을 위한 표현을 전부 제외하고, 오로지 자연어와 숫자로만 대답해.";

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

        // 4) Entity에서 DTO 재구성 (rawData 제거됨)
        java.util.List<RawAnalyticsDto.Persons> personsList = new java.util.ArrayList<>();
        for (com.pohanghang.spotline.domain.analytics.entity.AnalyticsPerson ap : analytics.getPersons()) {
            for (int i = 0; i < ap.getCount(); i++) {
                personsList.add(new RawAnalyticsDto.Persons(
                        null,
                        ap.getGender().name().toLowerCase(),
                        AGE_GROUP_LABELS.getOrDefault(ap.getAgeGroup(), "unknown"),
                        null, null, null, null
                ));
            }
        }

        return new RawAnalyticsDto(
                new RawAnalyticsDto.Summary(
                        analytics.getTotalCount(),
                        analytics.getPeakCongestion().name().toLowerCase(),
                        analytics.getAvgDwellTimeSeconds()
                ),
                personsList
        );
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
                .filter(group -> group.getAgeGroup() != AgeGroup.UNKNOWN && !"UNKNOWN".equals(group.getGender().name()))
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

        int totalCount = 0;
        for (AnalyticsRepository.HourlyPopulationGroup hourlyPopulationGroup : hourlyPopulationGroups) {
            int count = Math.toIntExact(hourlyPopulationGroup.getTotalCount());
            ageGroupCounts.put(
                    hourlyPopulationGroup.getAgeGroup(),
                    count
            );
            if (hourlyPopulationGroup.getAgeGroup() != AgeGroup.UNKNOWN) {
                totalCount += count;
            }
        }

        if (totalCount == 0) {
            return new AgeGroupDistributionDto(0, 0, 0, 0, 0, 0);
        }

        return new AgeGroupDistributionDto(
                (int) Math.round((double) ageGroupCounts.get(AgeGroup.CHILD) / totalCount * 100),
                (int) Math.round((double) ageGroupCounts.get(AgeGroup.TEN) / totalCount * 100),
                (int) Math.round((double) ageGroupCounts.get(AgeGroup.TWENTY) / totalCount * 100),
                (int) Math.round((double) ageGroupCounts.get(AgeGroup.THIRTY) / totalCount * 100),
                (int) Math.round((double) ageGroupCounts.get(AgeGroup.FORTY) / totalCount * 100),
                (int) Math.round((double) ageGroupCounts.get(AgeGroup.FIFTY_PLUS) / totalCount * 100)
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

        int yesterdayVisitsSum = 0;
        int yesterdayVisitsCount = 0;
        int lastWeekVisitsSum = 0;
        int lastWeekVisitsCount = 0;
        Weather yesterdayWeather = Weather.SUNNY;

        for (AnalyticsRepository.WeatherImpactRow row : rows) {
            if (row.getStartAt() == null || row.getTotalCount() == null) continue;
            LocalDate rowDate = row.getStartAt().toLocalDate();
            if (rowDate.equals(yesterday)) {
                yesterdayVisitsSum += row.getTotalCount();
                yesterdayVisitsCount++;
                if (row.getWeather() != null) yesterdayWeather = row.getWeather();
            } else if (rowDate.equals(yesterday.minusDays(7))) {
                lastWeekVisitsSum += row.getTotalCount();
                lastWeekVisitsCount++;
            }
        }
        
        int yesterdayVisits = yesterdayVisitsCount > 0 ? Math.round((float) yesterdayVisitsSum / yesterdayVisitsCount) : 0;
        int lastWeekVisits = lastWeekVisitsCount > 0 ? Math.round((float) lastWeekVisitsSum / lastWeekVisitsCount) : 0;
        
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
                SYSTEM_PROMPT +
                "어제 방문 %d명 (%s%d%%, z=%s, %s).\n" +
                "핵심 고객 %s.\n" +
                "평균 체류 %d분 (평소%s분).\n" +
                "날씨 %s, 보정 후 %s.\n" +
                "오늘 예측 %d명, 오후 %s 예보.\n" +
                "위 내용을 일일 브리핑 형식으로 변환해줘. 날짜 정보는 포함하지마.",
                yesterdayVisits, diffSign, diffPercent, zScoreStr, zScoreDesc,
                coreCustomerStr,
                yesterdayDwellMins, dwellDiffStr,
                weatherName, weatherResultDesc,
                todayPrediction.expectedVisits(), todayWeatherStr
        );

        String responseMessage = geminiClient.generateContent(prompt);
        return new MessageResponseDto(responseMessage);
    }

    @Transactional(readOnly = true)
    public MessageResponseDto getMarketingRecommendations() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        List<AnalyticsRepository.WeatherImpactRow> rows = analyticsRepository.findWeatherImpactRows();

        StringBuilder triggers = new StringBuilder();

        // [Rule 1] 특정 인구통계 감소
        LocalDateTime week0 = now.minusDays(21);
        LocalDateTime week1 = now.minusDays(42);
        List<AnalyticsRepository.CoreCustomerGroup> recent3W = analyticsRepository.findCoreCustomerGroups(week0, now);
        List<AnalyticsRepository.CoreCustomerGroup> prev3W = analyticsRepository.findCoreCustomerGroups(week1, week0);

        if (!recent3W.isEmpty() && !prev3W.isEmpty()) {
            AnalyticsRepository.CoreCustomerGroup topRecent = recent3W.get(0);
            long totalRecent = recent3W.stream().mapToLong(AnalyticsRepository.CoreCustomerGroup::getTotalCount).sum();
            double recentRatio = (double) topRecent.getTotalCount() / totalRecent;

            long totalPrev = prev3W.stream().mapToLong(AnalyticsRepository.CoreCustomerGroup::getTotalCount).sum();
            double prevRatio = 0;
            for (AnalyticsRepository.CoreCustomerGroup g : prev3W) {
                if (g.getAgeGroup() == topRecent.getAgeGroup() && g.getGender() == topRecent.getGender()) {
                    prevRatio = (double) g.getTotalCount() / totalPrev;
                    break;
                }
            }

            if (prevRatio - recentRatio > 0.05) {
                String genderStr = "MALE".equals(topRecent.getGender().name()) ? "남성" : ("FEMALE".equals(topRecent.getGender().name()) ? "여성" : "성별미상");
                String ageStr = AGE_GROUP_LABELS.getOrDefault(topRecent.getAgeGroup(), "알수없음");
                triggers.append(String.format("- %s %s 방문 3주 연속 감소 (-%d%%p). 개선을 위한 마케팅 제안 1줄\n", ageStr, genderStr, (int)((prevRatio - recentRatio) * 100)));
            }
        }

        // [Rule 2] 만성 한산 시간대
        LocalDateTime fourWeeksAgo = now.minusDays(28);
        java.util.Map<String, Integer> timeSlotCounts = new java.util.HashMap<>();
        int totalVisits4W = 0;
        for (AnalyticsRepository.WeatherImpactRow row : rows) {
            if (row.getStartAt() == null || row.getStartAt().isBefore(fourWeeksAgo) || row.getTotalCount() == null) continue;
            java.time.DayOfWeek dow = row.getStartAt().getDayOfWeek();
            int hour = row.getStartAt().getHour();
            String dowStr = switch (dow) {
                case MONDAY -> "월요일";
                case TUESDAY -> "화요일";
                case WEDNESDAY -> "수요일";
                case THURSDAY -> "목요일";
                case FRIDAY -> "금요일";
                case SATURDAY -> "토요일";
                case SUNDAY -> "일요일";
            };
            if (hour >= 14 && hour < 17) {
                String key = dowStr + " 오후 2~5시";
                timeSlotCounts.put(key, timeSlotCounts.getOrDefault(key, 0) + row.getTotalCount());
            }
            totalVisits4W += row.getTotalCount();
        }
        
        if (!timeSlotCounts.isEmpty()) {
            double avgPerSlot = (double) totalVisits4W / (7 * 3);
            for (java.util.Map.Entry<String, Integer> entry : timeSlotCounts.entrySet()) {
                if (entry.getValue() < avgPerSlot * 0.5) {
                    triggers.append(String.format("- %s 방문이 평균의 %d%%. 해당 시간대 매출 개선 제안 1줄\n", entry.getKey(), (int)(entry.getValue() / avgPerSlot * 100)));
                    break;
                }
            }
        }

        // [Rule 3] 데드크로스 감지
        try {
            VisitCountResponseDto trend = VisitTrendCalculator.calculateTrend(now.minusDays(60), now, rows);
            List<Integer> ma5List = trend.data().get(2);
            List<Integer> ma20List = trend.data().get(4);
            if (ma5List.size() >= 2 && ma20List.size() >= 2) {
                int lastIdx = ma5List.size() - 1;
                Integer currMa5 = ma5List.get(lastIdx);
                Integer currMa20 = ma20List.get(lastIdx);
                Integer prevMa5 = ma5List.get(lastIdx - 1);
                Integer prevMa20 = ma20List.get(lastIdx - 1);
                if (currMa5 != null && currMa20 != null && prevMa5 != null && prevMa20 != null) {
                    if (currMa5 < currMa20 && prevMa5 >= prevMa20) {
                        triggers.append("- 순수 방문 추세 하락 전환. 사장님에게 주의 환기 + 행동 제안 1줄\n");
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        // [Rule 4] 고날씨 민감도 + 우천 예보
        try {
            OpenMeteoClient.WeatherData tomorrowWeather = openMeteoClient.getSeoulWeatherData(now.plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0));
            if (tomorrowWeather.weather() == Weather.RAINY || tomorrowWeather.weather() == Weather.SNOW) {
                PerformanceResultResponseDto impact = WeatherImpactCalculator.calculate(new WeatherImpactRequestDto(now.minusDays(1).toLocalDate().atStartOfDay()), rows);
                if (impact.adjustedValue() > 0 && impact.expectValue() > 0 && impact.realValue() / impact.expectValue() < 0.75) {
                    triggers.append("- 우리 매장 날씨 민감도 높음. 내일 비 예보. 우천 대응 마케팅 1줄 제안\n");
                }
            }
        } catch (Exception e) {
            // ignore
        }

        if (triggers.length() == 0) {
            triggers.append("- 현재 특별한 하락세나 이상 신호가 없습니다. 꾸준한 성장을 위한 일반적인 마케팅 아이디어 1줄 제안해줘.\n");
        }

        String prompt = SYSTEM_PROMPT +
                "다음 상황(트리거)들을 분석하여 사장님을 위한 마케팅/운영 제안을 작성해줘.\n" +
                "각 제안은 💡 기호로 시작하고, 상황 설명 후 행동 제안을 2~3줄로 해줘.\n\n" +
                "상황:\n" + triggers.toString();

        String responseMessage = geminiClient.generateContent(prompt);
        return new MessageResponseDto(responseMessage);
    }

    @Transactional(readOnly = true)
    public DailyVisitCountResponseDto getDailyVisitCount(LocalDate date) {
        if (date == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        List<AnalyticsRepository.WeatherImpactRow> rows = analyticsRepository.findWeatherImpactRows();

        int visitSum = 0;
        int visitCount = 0;

        for (AnalyticsRepository.WeatherImpactRow row : rows) {
            if (row.getStartAt() == null || row.getTotalCount() == null) continue;
            if (row.getStartAt().toLocalDate().equals(date)) {
                visitSum += row.getTotalCount();
                visitCount++;
            }
        }

        int dailyVisits = visitCount > 0 ? Math.round((float) visitSum / visitCount) : 0;
        return new DailyVisitCountResponseDto(dailyVisits);
    }
}
