package com.pohanghang.spotline.global.infra.storage;

import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

/**
 * 파일과 폴더의 입출력을 담당하는 클래스
 */
@Component
public class StorageIoCore {

    /**
     * 파일을 저장하는 메서드
     * @param multipartFile 저장할 파일
     * @param path 저장할 위치
     */
    public void write(
            final MultipartFile multipartFile,
            final Path path
    ) {
        try {
            multipartFile.transferTo(path);
        } catch (final FileAlreadyExistsException ex) {
            // 동일 파일명 존재 에러
            throw new CustomException(ExceptionCode.FILE_ALREADY_EXIST);
        } catch (final AccessDeniedException ex) {
            // 접근 권한 에러
            throw new CustomException(ExceptionCode.STORAGE_ACCESS_DENIED);
        } catch (final NoSuchFileException ex) {
            // 저장할 위치가 존재하지 않는 경우
            throw new CustomException(ExceptionCode.PATH_NOT_FOUND);
        } catch (final IOException ex) {
            final String message = ex.getMessage();

            // 저장공간 부족 에러
            if (message != null && (message.contains("No space left") || message.contains("not enough space"))) {
                throw new CustomException(ExceptionCode.STORAGE_FULL);
            }

            // 알 수 없는 에러
            else {
                throw new CustomException(ExceptionCode.FILE_WRITE_ERROR, ex);
            }
        }
    }

    /**
     * 파일 이동하는 메서드
     * @param source 이동할 파일 경로
     * @param target 파일명이 포함된 목표 경로
     */
    public void move(
            final Path source,
            final Path target
    ) {
        try {
            Files.move(source, target);
        } catch (final FileAlreadyExistsException ex) {
            throw new CustomException(ExceptionCode.FILE_ALREADY_EXIST);
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FILE_WRITE_ERROR, ex);
        }
    }

    /**
     * 파일을 Resource의 형태로 읽어오는 메서드
     * @param path 파일의 절대 경로
     * @return Resource
     */
    public Resource readFileAsResource(final Path path) {

        // 1) 파일 존재하는지 확인
        if (Files.notExists(path)) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }

        // 2) 파일 Resource로
        return new FileSystemResource(path);
    }
}
