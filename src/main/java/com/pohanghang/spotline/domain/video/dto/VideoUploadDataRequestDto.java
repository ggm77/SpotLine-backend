package com.pohanghang.spotline.domain.video.dto;

import com.pohanghang.spotline.domain.video.entity.Weather;

import java.time.LocalDateTime;

public record VideoUploadDataRequestDto (
    LocalDateTime startAt,
    LocalDateTime endAt,
    Weather weather,
    String detail
) { }
