package com.pohanghang.spotline.domain.analytics.repository;

import com.pohanghang.spotline.domain.analytics.entity.Analytics;
import com.pohanghang.spotline.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
    Optional<Analytics> findByVideo(final Video video);
}
