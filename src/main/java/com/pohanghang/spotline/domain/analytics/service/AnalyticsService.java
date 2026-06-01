package com.pohanghang.spotline.domain.analytics.service;

import com.pohanghang.spotline.domain.analytics.dto.*;
import com.pohanghang.spotline.domain.analytics.entity.AgeGroup;
import com.pohanghang.spotline.domain.analytics.entity.Gender;
import com.pohanghang.spotline.domain.analytics.entity.Weather;
import com.pohanghang.spotline.domain.store.entity.Store;
import com.pohanghang.spotline.domain.store.service.StoreService;
import com.pohanghang.spotline.domain.analytics.model.AnalyticsRow;
import com.pohanghang.spotline.domain.analytics.model.CoreCustomerGroup;
import com.pohanghang.spotline.domain.analytics.util.PredictionTomorrowCalculator;
import com.pohanghang.spotline.domain.analytics.util.VisitTrendCalculator;
import com.pohanghang.spotline.domain.analytics.util.WeatherImpactCalculator;
import com.pohanghang.spotline.domain.analytics.util.WeekdayPatternCalculator;
import com.pohanghang.spotline.domain.video.entity.PerformanceResult;
import com.pohanghang.spotline.domain.vision.entity.VisionData;
import com.pohanghang.spotline.domain.vision.entity.VisionPerson;
import com.pohanghang.spotline.domain.vision.repository.VisionDataRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.gemini.GeminiClient;
import com.pohanghang.spotline.global.infra.openmeteo.OpenMeteoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * v1 통계 API. 비전 AI가 적재한 {@link VisionData} 스냅샷들을 집계한다.
 * (과거 영상 분석(Analytics) 대신 /api/v2/vision/data 로 받은 데이터를 사용)
 */
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

    private static final String SYSTEM_PROMPT = "지금부터 당신은 마케팅 전문가가 되어 사장님을 위한 마케팅/운영 제안을 전략적이게 제안합니다.\n볼드 표시를 포함한 각종 md파일을 위한 표현을 전부 제외하고, 오로지 자연어와 숫자로만 대답해.\n";

    private final VisionDataRepository visionDataRepository;
    private final OpenMeteoClient openMeteoClient;
    private final GeminiClient geminiClient;
    private final StoreService storeService;

    @Transactional(readOnly = true)
    public CoreCustomerResponseDto getCoreCustomers(final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {
        validateRange(defaultStartAtEndAtRequestDto);

        final List<CoreCustomerGroup> coreCustomerGroups = buildCoreCustomerGroups(
                defaultStartAtEndAtRequestDto.startAt(),
                defaultStartAtEndAtRequestDto.endAt()
        );

        return coreCustomerGroups.stream()
                .filter(group -> group.getAgeGroup() != AgeGroup.UNKNOWN && group.getGender() != Gender.UNKNOWN)
                .findFirst()
                .map(coreCustomerGroup -> new CoreCustomerResponseDto(
                        coreCustomerGroup.getGender().name(),
                        AGE_GROUP_LABELS.get(coreCustomerGroup.getAgeGroup())
                ))
                .orElseThrow(() -> new CustomException(ExceptionCode.ANALYTICS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public AgeGroupDistributionDto getHourlyPopulation(final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {
        validateRange(defaultStartAtEndAtRequestDto);

        final Map<AgeGroup, Integer> ageGroupCounts = new EnumMap<>(AgeGroup.class);
        for (AgeGroup ageGroup : AgeGroup.values()) {
            ageGroupCounts.put(ageGroup, 0);
        }

        for (VisionData visionData : visionDataRepository.findOverlappingWithPeople(defaultStartAtEndAtRequestDto.startAt(), defaultStartAtEndAtRequestDto.endAt())) {
            for (VisionPerson person : visionData.getPeople()) {
                final AgeGroup ageGroup = toAgeGroup(person.getAge());
                ageGroupCounts.merge(ageGroup, 1, Integer::sum);
            }
        }

        int totalCount = 0;
        for (Map.Entry<AgeGroup, Integer> entry : ageGroupCounts.entrySet()) {
            if (entry.getKey() != AgeGroup.UNKNOWN) {
                totalCount += entry.getValue();
            }
        }

        if (totalCount == 0) {
            return new AgeGroupDistributionDto(-1, -1, -1, -1, -1, -1);
        }

        return new AgeGroupDistributionDto(
                toPercent(ageGroupCounts.get(AgeGroup.CHILD), totalCount),
                toPercent(ageGroupCounts.get(AgeGroup.TEN), totalCount),
                toPercent(ageGroupCounts.get(AgeGroup.TWENTY), totalCount),
                toPercent(ageGroupCounts.get(AgeGroup.THIRTY), totalCount),
                toPercent(ageGroupCounts.get(AgeGroup.FORTY), totalCount),
                toPercent(ageGroupCounts.get(AgeGroup.FIFTY_PLUS), totalCount)
        );
    }

    @Transactional(readOnly = true)
    public PerformanceResultResponseDto getWeatherImpact(final WeatherImpactRequestDto weatherImpactRequestDto) {
        if (weatherImpactRequestDto == null || weatherImpactRequestDto.day() == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        return WeatherImpactCalculator.calculate(weatherImpactRequestDto, loadRows());
    }

    @Transactional(readOnly = true)
    public PerformanceResultResponseDto getWeekdayPatterns(final WeekdayPatternRequestDto weekdayPatternRequestDto) {
        if (weekdayPatternRequestDto == null || weekdayPatternRequestDto.day() == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        return WeekdayPatternCalculator.calculate(weekdayPatternRequestDto, loadRows());
    }

    @Transactional(readOnly = true)
    public VisitCountResponseDto getVisitCount(final DefaultStartAtEndAtRequestDto defaultStartAtEndAtRequestDto) {
        validateRange(defaultStartAtEndAtRequestDto);

        return VisitTrendCalculator.calculateTrend(
                defaultStartAtEndAtRequestDto.startAt(),
                defaultStartAtEndAtRequestDto.endAt(),
                loadRows()
        );
    }

    @Transactional(readOnly = true)
    public PredictionTomorrowResponseDto getPredictionTomorrow() {
        final List<AnalyticsRow> rows = loadRows();

        LocalDateTime tomorrowAfternoon = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .plusDays(1)
                .withHour(14)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        OpenMeteoClient.WeatherData weatherData = getWeatherData(tomorrowAfternoon);
        Weather tomorrowWeather = weatherData.weather();

        return PredictionTomorrowCalculator.calculate(rows, tomorrowWeather, tomorrowAfternoon.toLocalDate());
    }

    @Transactional(readOnly = true)
    public PredictionNextWeekResponseDto getPredictionNextWeek() {
        final List<AnalyticsRow> rows = loadRows();
        List<PredictionTomorrowResponseDto> nextWeekPredictions = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        for (int i = 1; i <= 7; i++) {
            LocalDateTime targetAfternoon = now.plusDays(i)
                    .withHour(14)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            OpenMeteoClient.WeatherData weatherData = getWeatherData(targetAfternoon);
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

        List<AnalyticsRow> rows = loadRows();

        int yesterdayVisitsSum = 0;
        int yesterdayVisitsCount = 0;
        int lastWeekVisitsSum = 0;
        int lastWeekVisitsCount = 0;
        Weather yesterdayWeather = Weather.SUNNY;

        for (AnalyticsRow row : rows) {
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

        List<CoreCustomerGroup> coreGroups = buildCoreCustomerGroups(yesterdayStart, yesterdayEnd);
        String coreCustomerStr = "데이터 없음";
        if (!coreGroups.isEmpty()) {
            CoreCustomerGroup topGroup = coreGroups.get(0);
            String genderStr = Gender.MALE == topGroup.getGender() ? "남성" : (Gender.FEMALE == topGroup.getGender() ? "여성" : "성별미상");
            String ageStr = AGE_GROUP_LABELS.getOrDefault(topGroup.getAgeGroup(), "알수없음");
            int totalYesterdayPersons = coreGroups.stream().mapToInt(g -> g.getTotalCount().intValue()).sum();
            int topPercent = totalYesterdayPersons > 0 ? (int) Math.round((double) topGroup.getTotalCount() / totalYesterdayPersons * 100) : 0;
            coreCustomerStr = String.format("%s %s %d%%", ageStr, genderStr, topPercent);
        }

        // 평균 체류시간(분) — VisionData.avgDwellTime 사용
        double yesterdayDwellSum = 0;
        int yesterdayDwellCount = 0;
        double overallDwellSum = 0;
        int overallDwellCount = 0;

        for (VisionData visionData : visionDataRepository.findAll()) {
            if (visionData.getAvgDwellTime() == null || visionData.getCapturedAt() == null) continue;
            overallDwellSum += visionData.getAvgDwellTime();
            overallDwellCount++;
            if (!visionData.getCapturedAt().isBefore(yesterdayStart) && visionData.getCapturedAt().isBefore(yesterdayEnd)) {
                yesterdayDwellSum += visionData.getAvgDwellTime();
                yesterdayDwellCount++;
            }
        }

        int yesterdayDwellMins = yesterdayDwellCount > 0 ? (int) Math.round(yesterdayDwellSum / yesterdayDwellCount) : 0;
        int overallDwellMins = overallDwellCount > 0 ? (int) Math.round(overallDwellSum / overallDwellCount) : 0;
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
            OpenMeteoClient.WeatherData todayWeather = getWeatherData(todayAfternoon);
            Weather twW = todayWeather.weather();
            todayWeatherStr = twW == Weather.SUNNY ? "맑음" : (twW == Weather.CLOUDY ? "흐림" : (twW == Weather.RAINY ? "비" : "눈"));
            todayPrediction = PredictionTomorrowCalculator.calculate(rows, twW, today);
        } catch (Exception e) {
            todayPrediction = new PredictionTomorrowResponseDto(yesterdayVisits, yesterdayVisits, yesterdayVisits);
        }

        Store store = storeService.getDefaultStore();
        String storeContext = store != null
                ? String.format("가게명: %s, 업종: %s\n", store.getStoreName(), store.getBusinessType())
                : "";

        String prompt = String.format(
                SYSTEM_PROMPT + storeContext +
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
        List<AnalyticsRow> rows = loadRows();

        StringBuilder triggers = new StringBuilder();

        // [Rule 1] 특정 인구통계 감소
        LocalDateTime week0 = now.minusDays(21);
        LocalDateTime week1 = now.minusDays(42);
        List<CoreCustomerGroup> recent3W = buildCoreCustomerGroups(week0, now);
        List<CoreCustomerGroup> prev3W = buildCoreCustomerGroups(week1, week0);

        if (!recent3W.isEmpty() && !prev3W.isEmpty()) {
            CoreCustomerGroup topRecent = recent3W.get(0);
            long totalRecent = recent3W.stream().mapToLong(CoreCustomerGroup::getTotalCount).sum();
            double recentRatio = totalRecent > 0 ? (double) topRecent.getTotalCount() / totalRecent : 0;

            long totalPrev = prev3W.stream().mapToLong(CoreCustomerGroup::getTotalCount).sum();
            double prevRatio = 0;
            for (CoreCustomerGroup g : prev3W) {
                if (g.getAgeGroup() == topRecent.getAgeGroup() && g.getGender() == topRecent.getGender()) {
                    prevRatio = totalPrev > 0 ? (double) g.getTotalCount() / totalPrev : 0;
                    break;
                }
            }

            if (prevRatio - recentRatio > 0.05) {
                String genderStr = Gender.MALE == topRecent.getGender() ? "남성" : (Gender.FEMALE == topRecent.getGender() ? "여성" : "성별미상");
                String ageStr = AGE_GROUP_LABELS.getOrDefault(topRecent.getAgeGroup(), "알수없음");
                triggers.append(String.format("- %s %s 방문 3주 연속 감소 (-%d%%p). 개선을 위한 마케팅 제안 1줄\n", ageStr, genderStr, (int)((prevRatio - recentRatio) * 100)));
            }
        }

        // [Rule 2] 만성 한산 시간대
        LocalDateTime fourWeeksAgo = now.minusDays(28);
        Map<String, Integer> timeSlotCounts = new java.util.HashMap<>();
        int totalVisits4W = 0;
        for (AnalyticsRow row : rows) {
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
            for (Map.Entry<String, Integer> entry : timeSlotCounts.entrySet()) {
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
            OpenMeteoClient.WeatherData tomorrowWeather = getWeatherData(now.plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0));
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

        Store store = storeService.getDefaultStore();
        String storeContext = store != null
                ? String.format("가게명: %s, 업종: %s\n", store.getStoreName(), store.getBusinessType())
                : "";

        String prompt = SYSTEM_PROMPT + storeContext +
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

        int totalVisits = 0;
        boolean found = false;

        for (AnalyticsRow row : loadRows()) {
            if (row.getStartAt() == null || row.getTotalCount() == null) continue;
            if (row.getStartAt().toLocalDate().equals(date)) {
                totalVisits += row.getTotalCount();
                found = true;
            }
        }

        return new DailyVisitCountResponseDto(found ? totalVisits : -1);
    }

    // ===== VisionData 기반 데이터 빌더 =====

    /** 모든 비전 스냅샷을 통계 계산기 입력(AnalyticsRow)으로 변환한다. */
    private List<AnalyticsRow> loadRows() {
        final List<AnalyticsRow> rows = new ArrayList<>();
        for (VisionData visionData : visionDataRepository.findAll()) {
            rows.add(new AnalyticsRow(
                    visionData.getCapturedAt(),
                    visionData.getTotalCount(),
                    visionData.getWeather(),
                    visionData.getTemperature()
            ));
        }
        return rows;
    }

    /** 구간 내 방문자들을 (성별, 나이대)별로 집계해 방문자 수 내림차순으로 반환한다. */
    private List<CoreCustomerGroup> buildCoreCustomerGroups(final LocalDateTime startAt, final LocalDateTime endAt) {
        final Map<Gender, Map<AgeGroup, Long>> counts = new EnumMap<>(Gender.class);
        for (VisionData visionData : visionDataRepository.findOverlappingWithPeople(startAt, endAt)) {
            for (VisionPerson person : visionData.getPeople()) {
                final Gender gender = toGender(person.getGender());
                final AgeGroup ageGroup = toAgeGroup(person.getAge());
                counts.computeIfAbsent(gender, g -> new EnumMap<>(AgeGroup.class))
                        .merge(ageGroup, 1L, Long::sum);
            }
        }

        final List<CoreCustomerGroup> groups = new ArrayList<>();
        for (Map.Entry<Gender, Map<AgeGroup, Long>> genderEntry : counts.entrySet()) {
            for (Map.Entry<AgeGroup, Long> ageEntry : genderEntry.getValue().entrySet()) {
                groups.add(new CoreCustomerGroup(genderEntry.getKey(), ageEntry.getKey(), ageEntry.getValue()));
            }
        }
        groups.sort(Comparator.comparingLong(CoreCustomerGroup::getTotalCount).reversed());
        return groups;
    }

    private void validateRange(final DefaultStartAtEndAtRequestDto requestDto) {
        if (requestDto == null
                || requestDto.startAt() == null
                || requestDto.endAt() == null
                || !requestDto.startAt().isBefore(requestDto.endAt())) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }
    }

    private int toPercent(final Integer count, final int total) {
        return (int) Math.round((double) (count == null ? 0 : count) / total * 100);
    }

    private Gender toGender(final Integer gender) {
        if (gender == null) {
            return Gender.UNKNOWN;
        }
        return switch (gender) {
            case 1 -> Gender.MALE;
            case 2 -> Gender.FEMALE;
            default -> Gender.UNKNOWN;
        };
    }

    private AgeGroup toAgeGroup(final Integer age) {
        if (age == null || age < 0) {
            return AgeGroup.UNKNOWN;
        }
        return switch (age / 10) {
            case 0 -> AgeGroup.CHILD;
            case 1 -> AgeGroup.TEN;
            case 2 -> AgeGroup.TWENTY;
            case 3 -> AgeGroup.THIRTY;
            case 4 -> AgeGroup.FORTY;
            default -> AgeGroup.FIFTY_PLUS;
        };
    }

    private OpenMeteoClient.WeatherData getWeatherData(final LocalDateTime targetAt) {
        Store store = storeService.getDefaultStore();
        if (store != null && store.getLatitude() != null && store.getLongitude() != null) {
            return openMeteoClient.getWeatherData(store.getLatitude(), store.getLongitude(), targetAt);
        }
        return openMeteoClient.getSeoulWeatherData(targetAt);
    }
}
