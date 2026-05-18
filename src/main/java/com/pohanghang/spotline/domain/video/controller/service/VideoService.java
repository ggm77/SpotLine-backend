package com.pohanghang.spotline.domain.video.controller.service;

import com.pohanghang.spotline.domain.video.dto.VideoStatusResponseDto;
import com.pohanghang.spotline.domain.video.dto.VideoUploadResponseDto;
import com.pohanghang.spotline.domain.video.entity.Status;
import com.pohanghang.spotline.domain.video.entity.Video;
import com.pohanghang.spotline.domain.video.repository.VideoRepository;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.storage.StorageManager;
import com.pohanghang.spotline.global.infra.storage.VideoValidator;
import com.pohanghang.spotline.global.infra.video.VideoAnalyze;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final StorageManager storageManager;
    private final VideoAnalyze videoAnalyze;

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
        if (!VideoValidator.isValidMp4(path)){
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 4) 엔티티 생성
        final Video video = Video.builder()
                .name(path.getFileName().toString())
                .startAt(startAt)
                .endAt(endAt)
                .status(Status.PROCESSING)
                .build();

        // 5) DB 저장
        final Video savedVideo = videoRepository.save(video);

        // 6) 비디오 분석 시작
        videoAnalyze.analyze(video, startAt, endAt);

        return new VideoUploadResponseDto(savedVideo.getId());
    }

    public Resource downloadVideo(final Long id) {
        // 1) null 검사
        if (id == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 영상 조회
        final Video video = videoRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.VIDEO_NOT_FOUND));

        // 3) 파일 리소스 가져오기
        return storageManager.getFile(video.getName());
    }

    public VideoStatusResponseDto getStatus(final Long id) {
        // 1) null 검사
        if (id == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 영상 조회
        final Video video = videoRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.VIDEO_NOT_FOUND));

        // 3) 결과 리턴
        return new VideoStatusResponseDto(video.getStatus());
    }
}
