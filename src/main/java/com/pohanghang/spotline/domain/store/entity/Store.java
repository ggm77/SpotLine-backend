package com.pohanghang.spotline.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String storeName;

    @Column(length = 100, nullable = false)
    private String businessType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Store(final String storeName, final String businessType) {
        this.storeName = storeName;
        this.businessType = businessType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(final String storeName, final String businessType) {
        this.storeName = storeName;
        this.businessType = businessType;
        this.updatedAt = LocalDateTime.now();
    }
}
