package com.pohanghang.spotline.domain.store.dto;

import com.pohanghang.spotline.domain.store.entity.Store;

public record StoreResponseDto(
        String storeName,
        String businessType,
        Double latitude,
        Double longitude
) {
    public static StoreResponseDto from(final Store store) {
        return new StoreResponseDto(
                store.getStoreName(),
                store.getBusinessType(),
                store.getLatitude(),
                store.getLongitude()
        );
    }
}
