package com.pohanghang.spotline.global.util;

import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 날짜·시간 문자열 파싱 유틸.
 *
 * <p>"2026-05-17T15:00:00" (오프셋 없음)과 "2026-05-17T15:00:00.000Z" (Z/오프셋 포함)
 * 두 형식을 모두 허용한다. 오프셋(Z, +09:00 등)이 있으면 무시하고 벽시계 기준
 * {@link LocalDateTime} 으로 변환한다. (두 형식을 동일한 시각으로 취급)</p>
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static LocalDateTime parseFlexible(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            // ISO_DATE_TIME 은 오프셋 유무를 모두 허용하고, LocalDateTime 변환 시 오프셋은 무시된다.
            return LocalDateTime.parse(raw.trim(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (final DateTimeParseException ex) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }
    }
}
