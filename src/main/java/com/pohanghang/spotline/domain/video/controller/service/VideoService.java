package com.pohanghang.spotline.domain.video.controller.service;

import com.pohanghang.spotline.domain.video.dto.VideoUploadResponseDto;
import com.pohanghang.spotline.domain.video.entity.Video;
import com.pohanghang.spotline.domain.video.repository.VideoRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.storage.StorageManager;
import com.pohanghang.spotline.global.infra.storage.VideoAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final StorageManager storageManager;

    public VideoUploadResponseDto uploadVideo(
            final MultipartFile multipartFile,
            final LocalDateTime startAt,
            final LocalDateTime endAt
    ) {
        // 1) 파라미터 검사
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }
        if (startAt == null || endAt == null || endAt.isBefore(startAt)) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 파일 저장
        final Path path = storageManager.save(multipartFile);

        // 3) 정상 mp4인지 검사
        if (!VideoAnalyzer.isValidMp4(path)){
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 4) 엔티티 생성
        final Video video = Video.builder()
                .name(path.getFileName().toString())
                .startAt(startAt)
                .endAt(endAt)
                .build();

        // 5) DB 저장
        return new VideoUploadResponseDto(
                videoRepository.save(video).getId()
        );
    }
}
