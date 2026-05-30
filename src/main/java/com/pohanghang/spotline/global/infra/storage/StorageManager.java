package com.pohanghang.spotline.global.infra.storage;

import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 파일이나 폴더의 저장, 조회, 이동, 삭제 등을 제공하는 클래스
 * 이 클래스를 통해서 서비스 레이어에서 파일 작업을 함
 */
@Component
@RequiredArgsConstructor
public class StorageManager {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Value("${save-path}")
    private String SAVE_PATH;

    private final StorageIoCore storageIoCore;

    /**
     * 스트리밍으로 들어온 영상 청크를 저장하는 메서드.
     * 청크는 촬영 시점(createdAt) 순으로 정렬해서 사용해야 하므로,
     * 파일명 앞에 촬영 시점의 epochMilli 를 붙여 정렬 가능하게 저장한다.
     * @param multipartFile 청크로 나뉜 영상 파일
     * @param createdAt 영상이 찍힌 시점
     * @return 저장된 파일의 절대 경로
     */
    public Path saveStreamChunk(
            final MultipartFile multipartFile,
            final LocalDateTime createdAt
    ) {
        // 1) null 체크
        if (multipartFile == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }
        if (createdAt == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 확장자 추출
        final String extension = extractExtension(multipartFile.getOriginalFilename());

        // 3) 촬영 시점 기준 정렬이 가능하도록 epochMilli 를 접두어로 사용
        final long epochMilli = createdAt.atZone(KST).toInstant().toEpochMilli();
        final String name = "chunk_" + epochMilli + "_" + UUID.randomUUID()
                + (extension.isBlank() ? "" : "." + extension);

        // 4) 저장할 경로 생성
        final Path path = toPath(SAVE_PATH, name);

        // 5) 저장
        storageIoCore.write(multipartFile, path);

        return path;
    }

    private Path toPath(
            final String base,
            final String fileName
    ) {
        // 1) null 검사
        if (fileName == null || fileName.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) base랑 합쳐서 경로로
        final Path basePath = Path.of(base).toAbsolutePath().normalize();
        final Path path = basePath.resolve(fileName).normalize();

        // 4) 경로 검사
        if (!path.startsWith(basePath)) {
            throw new CustomException(ExceptionCode.STORAGE_ACCESS_DENIED);
        }

        return path;
    }

    private String extractExtension(final String fileName) {
        // 1) null 체크
        if (fileName == null || fileName.isBlank()){
            return "";
        }

        // 2) 확장자 존재 확인
        final int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1){
            return "";
        }

        return fileName.substring(dotIndex + 1);
    }
}

