package com.pohanghang.spotline.domain.analytics.service;

import com.pohanghang.spotline.domain.analytics.dto.RawAnalyticsDto;
import com.pohanghang.spotline.domain.analytics.entity.Analytics;
import com.pohanghang.spotline.domain.analytics.repository.AnalyticsRepository;
import com.pohanghang.spotline.domain.video.entity.Video;
import com.pohanghang.spotline.domain.video.repository.VideoRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final VideoRepository videoRepository;

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
}
