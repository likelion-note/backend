package com.likelion.notebook.global.exception;

import com.likelion.notebook.global.common.ErrorCode;
import lombok.Getter;

/**
 * 서비스 코드에서는 이것만 던지기.
 *   throw new BusinessException(ErrorCode.NOT_FOUND);                         // 기본 문구
 *   throw new BusinessException(ErrorCode.INVALID_INPUT, "사유를 한 줄 적어 주세요"); // 화면에 보일 문구 지정
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
