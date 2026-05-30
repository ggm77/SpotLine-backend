package com.pohanghang.spotline.domain.video.service;

import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final StorageManager storageManager;

    /**
     * 프론트에서 스트리밍으로 보내는 짧은 영상 청크를 저장한다.
     * 청크는 촬영 시점(createdAt)으로 정렬 가능하게 저장되며, 이후 정렬·결합해 사용한다.
     */
    public void saveStreamChunk(
            final LocalDateTime createdAt,
            final MultipartFile fileChunk
    ) {
        // 1) 파라미터 검사
        if (createdAt == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }
        if (fileChunk == null || fileChunk.isEmpty()) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 청크 저장
        storageManager.saveStreamChunk(fileChunk, createdAt);
    }
}
