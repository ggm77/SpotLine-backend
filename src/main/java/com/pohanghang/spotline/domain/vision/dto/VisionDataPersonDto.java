package com.pohanghang.spotline.domain.vision.dto;

import java.time.LocalDateTime;

public record VisionDataPersonDto(
        Long id,
        LocalDateTime in,
        LocalDateTime out
) { }
