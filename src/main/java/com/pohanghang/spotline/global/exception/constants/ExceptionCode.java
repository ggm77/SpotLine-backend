package com.pohanghang.spotline.global.exception.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "필요한 값이 비어있습니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "올바르지 않은 파일입니다."),
    STORAGE_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "저장소에 대한 접근 권한이 없습니다."),
    PATH_NOT_FOUND(HttpStatus.BAD_REQUEST, "경로를 찾을 수 없습니다."),
    FILE_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "동일한 이름의 파일이 존재합니다."),
    FOLDER_NOT_EXIST(HttpStatus.BAD_REQUEST, "폴더가 존재하지 않습니다."),
    FILE_NOT_EXIST(HttpStatus.BAD_REQUEST, "파일이 존재하지 않습니다."),
    VIDEO_NOT_FOUND(HttpStatus.BAD_REQUEST, "영상이 존재하지 않습니다."),
    ANALYTICS_NOT_FOUND(HttpStatus.BAD_REQUEST, "분석 결과를 찾을 수 없습니다."),
    STORE_NOT_FOUND(HttpStatus.BAD_REQUEST, "가게 정보를 찾을 수 없습니다."),
    VISION_DATA_NOT_FOUND(HttpStatus.BAD_REQUEST, "비전 데이터를 찾을 수 없습니다."),

    FFMPEG_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FFmpeg에서 오류가 발생했습니다."),
    PROCESS_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "프로세스 실행이 중단되었습니다."),
    COMMAND_TIMEOUT(HttpStatus.INTERNAL_SERVER_ERROR, "명령어 처리중 타임아웃이 발생했습니다."),
    FILE_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."),
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽어오는 중 오류가 발생했습니다."),
    STORAGE_FULL(HttpStatus.INTERNAL_SERVER_ERROR, "저장공간이 부족합니다."),
    FOLDER_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "폴더 저장 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 에러가 발생했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
