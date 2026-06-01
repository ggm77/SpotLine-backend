package com.pohanghang.spotline.domain.store.dto;

public record StoreRequestDto(
        String storeName,
        String businessType,
        Double latitude,
        Double longitude
) {
}
