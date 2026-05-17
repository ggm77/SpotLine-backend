package com.pohanghang.spotline.global.exception;

import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ExceptionCode exceptionCode;

    public CustomException(final ExceptionCode exceptionCode) {
        super("");
        this.exceptionCode = exceptionCode;
    }

    public CustomException(final ExceptionCode exceptionCode, final Exception exception) {
        super(exception.getMessage(), exception);
        this.exceptionCode = exceptionCode;
    }
}

