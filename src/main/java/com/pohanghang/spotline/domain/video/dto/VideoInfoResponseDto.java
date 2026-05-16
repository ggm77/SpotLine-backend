package com.pohanghang.spotline.domain.video.dto;

import com.pohanghang.spotline.domain.video.entity.Weather;

import java.time.LocalDateTime;
import java.util.List;

public record VideoInfoResponseDto(
        Long id,
        Integer visitorCount,
        Integer congestionLevel,
        Integer dwellTime,
        Integer manCount,
        Integer womanCount,
        Integer doorwayEventCount,
        Integer counterEventCount,
        List<Integer> ageCount,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Weather weather
) { }
