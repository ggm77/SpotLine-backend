package com.pohanghang.spotline.domain.vision.repository;

import com.pohanghang.spotline.domain.vision.entity.VisionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisionDataRepository extends JpaRepository<VisionData, Long> {

    /**
     * 조회 구간과 겹치는 스냅샷들을 조회한다. (사람 정보는 로드하지 않음 — 스칼라 집계용)
     * (스냅샷의 capturedAt~endAt 구간이 [startAt, endAt)와 겹치면 포함)
     */
    @Query("""
            SELECT v FROM VisionData v
            WHERE v.capturedAt < :endAt
              AND v.endAt > :startAt
            """)
    List<VisionData> findOverlapping(
            @Param("startAt") final LocalDateTime startAt,
            @Param("endAt") final LocalDateTime endAt
    );

    /**
     * 조회 구간과 겹치는 스냅샷들을 사람(people) 정보까지 함께 조회한다. (사람 단위 집계용)
     */
    @Query("""
            SELECT DISTINCT v FROM VisionData v
            LEFT JOIN FETCH v.people
            WHERE v.capturedAt < :endAt
              AND v.endAt > :startAt
            """)
    List<VisionData> findOverlappingWithPeople(
            @Param("startAt") final LocalDateTime startAt,
            @Param("endAt") final LocalDateTime endAt
    );

    Optional<VisionData> findTopByOrderByCapturedAtDesc();

    Optional<VisionData> findTopByOrderByCreatedAtDesc();
}
