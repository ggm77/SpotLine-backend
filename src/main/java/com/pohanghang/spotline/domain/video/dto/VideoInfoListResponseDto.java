package com.pohanghang.spotline.domain.video.dto;

import java.util.List;

public record VideoInfoListResponseDto(
        List<VideoInfoResponseDto> videoList
) { }
