package com.pohanghang.spotline.domain.video.entity;

import com.pohanghang.spotline.domain.analytics.entity.Analytics;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Analytics> analytics;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Video(
            final String name,
            final LocalDateTime startAt,
            final LocalDateTime endAt
    ) {
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
