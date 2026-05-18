package com.pohanghang.spotline.domain.analytics.repository;

import com.pohanghang.spotline.domain.analytics.entity.Analytics;
import com.pohanghang.spotline.domain.analytics.entity.AgeGroup;
import com.pohanghang.spotline.domain.analytics.entity.Gender;
import com.pohanghang.spotline.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
    Optional<Analytics> findByVideo(final Video video);

    @Query("""
            SELECT p.gender AS gender,
                   p.ageGroup AS ageGroup,
                   SUM(p.count) AS totalCount
            FROM Analytics a
            JOIN a.persons p
            WHERE a.startAt < :endAt
              AND a.endAt > :startAt
            GROUP BY p.gender, p.ageGroup
            ORDER BY SUM(p.count) DESC
            """)
    List<CoreCustomerGroup> findCoreCustomerGroups(
            @Param("startAt") final LocalDateTime startAt,
            @Param("endAt") final LocalDateTime endAt
    );

    interface CoreCustomerGroup {
        Gender getGender();

        AgeGroup getAgeGroup();

        Long getTotalCount();
    }
}
