package com.likelion.notebook.global.exception;

import com.likelion.notebook.global.common.ErrorCode;
import lombok.Getter;

/** 서비스 코드에서는 이것만 던지기: throw new BusinessException(ErrorCode.JOURNAL_NOT_FOUND); */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
