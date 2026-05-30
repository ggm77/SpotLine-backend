package com.pohanghang.spotline.global.infra.video;

import com.pohanghang.spotline.domain.analytics.dto.RawAnalyticsDto;
import com.pohanghang.spotline.domain.analytics.entity.*;
import com.pohanghang.spotline.global.util.JsonUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RawAnalysisConverter {

    public static Analytics toEntity(
            final RawAnalyticsDto rawAnalyticsDto,
            final LocalDateTime startAt,
            final LocalDateTime endAt
    ) {
        final RawAnalyticsDto.Summary summary = rawAnalyticsDto.summary();

        final Analytics analytics = Analytics.builder()
                .startAt(startAt)
                .endAt(endAt)
                .peakCongestion(toCongestion(summary.peakCongestion()))
                .avgDwellTimeSeconds(summary.avgDwellTimeSeconds())
                .totalCount(summary.totalVisitors())
                .rawData(JsonUtil.toJson(rawAnalyticsDto))
                .build();

        final List<AnalyticsPerson> personList = toAnalyticsPersons(rawAnalyticsDto.persons(), analytics);
        analytics.getPersons().addAll(personList);

        return analytics;
    }

    private static List<AnalyticsPerson> toAnalyticsPersons(
            final List<RawAnalyticsDto.Persons> persons,
            final Analytics analytics
    ) {
        // (Gender, AgeGroup) 쌍으로 그룹핑하여 count 집계
        final Map<String, Integer> countMap = new LinkedHashMap<>();
        for (RawAnalyticsDto.Persons person : persons) {
            final String key = person.gender() + ":" + person.ageGroup();
            countMap.merge(key, 1, Integer::sum);
        }

        final List<AnalyticsPerson> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            final String[] parts = entry.getKey().split(":");
            result.add(AnalyticsPerson.builder()
                    .analytics(analytics)
                    .gender(toGender(parts[0]))
                    .ageGroup(toAgeGroup(parts[1]))
                    .count(entry.getValue())
                    .build());
        }
        return result;
    }

    private static Congestion toCongestion(final String value) {
        return switch (value.toLowerCase()) {
            case "low" -> Congestion.LOW;
            case "medium" -> Congestion.MEDIUM;
            case "high" -> Congestion.HIGH;
            default -> throw new IllegalArgumentException("Unknown congestion: " + value);
        };
    }

    private static Gender toGender(final String value) {
        return switch (value.toLowerCase()) {
            case "male" -> Gender.MALE;
            case "female" -> Gender.FEMALE;
            case "unknown" -> Gender.UNKNOWN;
            default -> throw new IllegalArgumentException("Unknown gender: " + value);
        };
    }

    private static AgeGroup toAgeGroup(final String value) {
        return switch (value.toLowerCase()) {
            case "00s" -> AgeGroup.CHILD;
            case "10s" -> AgeGroup.TEN;
            case "20s" -> AgeGroup.TWENTY;
            case "30s" -> AgeGroup.THIRTY;
            case "40s" -> AgeGroup.FORTY;
            case "50s+" -> AgeGroup.FIFTY_PLUS;
            case "unknown" -> AgeGroup.UNKNOWN;
            default -> throw new IllegalArgumentException("Unknown ageGroup: " + value);
        };
    }
}

