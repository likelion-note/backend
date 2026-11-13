package com.likelion.notebook.global.exception;

import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.global.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_INPUT, msg));
    }

    /** JSON 형식 오류, 값 목록에 없는 enum 값("part": "XYZ"), 경로 변수 타입 오류(/api/journals/abc) */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }
}
