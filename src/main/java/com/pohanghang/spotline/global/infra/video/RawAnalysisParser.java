package com.pohanghang.spotline.global.infra.video;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.pohanghang.spotline.domain.analytics.dto.RawAnalyticsDto;

import java.util.List;

/**
 * Python YOLO 분석 서버에서 반환하는 DailyAnalysis JSON을
 * RawAnalyticsDto로 파싱하는 유틸리티 클래스.
 *
 * <p>Python Pydantic 모델(DailyAnalysis)의 snake_case JSON 구조를
 * 내부 record로 역직렬화한 뒤, 기존 RawAnalyticsDto로 변환한다.</p>
 */
public class RawAnalysisParser {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ========== Python JSON 구조에 대응하는 내부 record ==========

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DailyAnalysis(
            String date,
            VideoMetadata videoMetadata,
            PythonSummary summary,
            List<PersonRecord> persons,
            List<CongestionPoint> congestionTimeline
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoMetadata(
            String filename,
            Double durationSeconds,
            Double fps,
            String resolution,
            String processedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GenderDistribution(
            Integer male,
            Integer female,
            Integer unknown
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgeDistribution(
            Integer zeros,
            Integer tens,
            Integer twenties,
            Integer thirties,
            Integer forties,
            Integer fiftiesPlus,
            Integer unknown
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PythonSummary(
            Integer totalVisitors,
            String peakCongestion,
            Double avgDwellTimeSeconds,
            GenderDistribution genderDistribution,
            AgeDistribution ageDistribution
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonRecord(
            Integer trackId,
            String gender,
            String ageGroup,
            String firstSeen,
            String lastSeen,
            Double dwellTimeSeconds,
            EntranceEventRecord entranceEvent
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EntranceEventRecord(
            String event,
            String timestamp
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CongestionPoint(
            String timestamp,
            Integer personCount
    ) {}

    // ========== 파싱 메서드 ==========

    /**
     * Python DailyAnalysis JSON 문자열을 RawAnalyticsDto로 변환한다.
     *
     * @param json Python 분석 서버가 반환한 JSON 문자열
     * @return 기존 시스템에서 사용하는 RawAnalyticsDto
     */
    public static RawAnalyticsDto parse(final String json) {
        try {
            final DailyAnalysis dailyAnalysis = objectMapper.readValue(json, DailyAnalysis.class);
            return toRawAnalyticsDto(dailyAnalysis);
        } catch (Exception e) {
            throw new RuntimeException("DailyAnalysis JSON 파싱 실패", e);
        }
    }

    // ========== 내부 변환 로직 ==========

    private static RawAnalyticsDto toRawAnalyticsDto(final DailyAnalysis dailyAnalysis) {
        final PythonSummary src = dailyAnalysis.summary();

        final RawAnalyticsDto.Summary summary = new RawAnalyticsDto.Summary(
                src.totalVisitors(),
                src.peakCongestion(),
                src.avgDwellTimeSeconds()
        );

        final List<RawAnalyticsDto.Persons> persons = dailyAnalysis.persons().stream()
                .map(RawAnalysisParser::toPersons)
                .toList();

        return new RawAnalyticsDto(summary, persons);
    }

    private static RawAnalyticsDto.Persons toPersons(final PersonRecord p) {
        final RawAnalyticsDto.EntranceEvent entranceEvent = p.entranceEvent() != null
                ? new RawAnalyticsDto.EntranceEvent(p.entranceEvent().event(), p.entranceEvent().timestamp())
                : null;

        return new RawAnalyticsDto.Persons(
                p.trackId(),
                p.gender(),
                p.ageGroup(),
                p.firstSeen(),
                p.lastSeen(),
                p.dwellTimeSeconds(),
                entranceEvent
        );
    }
}
