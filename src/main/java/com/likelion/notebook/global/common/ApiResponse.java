package com.likelion.notebook.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API 응답 형식. 프론트와 이 모양으로 약속.
 * 성공: { "success": true,  "data": {...} }
 * 실패: { "success": false, "code": "JOURNAL_NOT_FOUND", "message": "..." }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String code, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, errorCode.name(), errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode.name(), message);
    }
}
