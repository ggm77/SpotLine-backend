package com.pohanghang.spotline.domain.analytics.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyticsPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analytics_id", nullable = false)
    private Analytics analytics;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgeGroup ageGroup;

    @Column(nullable = false)
    private Integer count;

    @Builder
    public AnalyticsPerson(
            final Analytics analytics,
            final Gender gender,
            final AgeGroup ageGroup,
            final Integer count
    ) {
        this.analytics = analytics;
        this.gender = gender;
        this.ageGroup = ageGroup;
        this.count = count;
    }
}
