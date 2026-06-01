package com.pohanghang.spotline.domain.store.service;

import com.pohanghang.spotline.domain.store.dto.StoreRequestDto;
import com.pohanghang.spotline.domain.store.dto.StoreResponseDto;
import com.pohanghang.spotline.domain.store.entity.Store;
import com.pohanghang.spotline.domain.store.repository.StoreRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional(readOnly = true)
    public StoreResponseDto getStore() {
        return StoreResponseDto.from(findStore());
    }

    @Transactional
    public StoreResponseDto upsertStore(final StoreRequestDto request) {
        Store store = storeRepository.findAll().stream().findFirst().orElse(null);

        if (store == null) {
            store = storeRepository.save(Store.builder()
                    .storeName(request.storeName())
                    .businessType(request.businessType())
                    .build());
        } else {
            store.update(request.storeName(), request.businessType());
        }

        return StoreResponseDto.from(store);
    }

    @Transactional
    public void deleteStore() {
        storeRepository.delete(findStore());
    }

    public Store getDefaultStore() {
        return storeRepository.findAll().stream().findFirst().orElse(null);
    }

    private Store findStore() {
        return storeRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new CustomException(ExceptionCode.STORE_NOT_FOUND));
    }
}
