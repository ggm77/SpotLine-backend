package com.pohanghang.spotline.domain.store.controller;

import com.pohanghang.spotline.domain.store.dto.StoreRequestDto;
import com.pohanghang.spotline.domain.store.dto.StoreResponseDto;
import com.pohanghang.spotline.domain.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/store")
@RestController
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<StoreResponseDto> getStore() {
        return ResponseEntity.ok(storeService.getStore());
    }

    @PutMapping
    public ResponseEntity<StoreResponseDto> upsertStore(
            @RequestBody final StoreRequestDto request) {
        return ResponseEntity.ok(storeService.upsertStore(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteStore() {
        storeService.deleteStore();
        return ResponseEntity.noContent().build();
    }
}
