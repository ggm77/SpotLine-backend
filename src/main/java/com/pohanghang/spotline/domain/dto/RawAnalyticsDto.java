package com.pohanghang.spotline.domain.dto;

import java.util.Arrays;
import java.util.List;

public record RawAnalyticsDto(
        Summary summary
) {

    public record Summary(
            Integer totalVisitors,
            String peakCongestion,
            Double avgDwellTimeSeconds,
            List<Persons> persons
    ) {}

    public record Persons(
            Integer trackId,
            String gender,
            String ageGroup,
            String firstSeen,
            String last_seen,
            Double dwellTimeSeconds,
            EntranceEvent entranceEvent
    ) {}

    public record EntranceEvent(
            String event,
            String timestamp
    ) {}

    public static RawAnalyticsDto getMock() {
        return new RawAnalyticsDto(
                new Summary(
                        76, "medium", 11.242,
                        Arrays.asList(new Persons(
                                1,
                                "female",
                                "20s",
                                "00:00:00.000",
                                "00:00:19.476",
                                19.476,
                                new EntranceEvent("enter", "00:00:19.476")
                        ))
                )
        );
    }
}
