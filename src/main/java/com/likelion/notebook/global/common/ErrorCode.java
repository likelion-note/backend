package com.likelion.notebook.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드는 이 5개만 쓰기로 함 (팀 결정). 새 코드 추가하지 않기.
 * 상황별 문구는 코드 대신 메시지로 구분:
 *   throw new BusinessException(ErrorCode.INVALID_INPUT, "막힌 것을 30자 이상 적어 주세요");
 * 메시지를 안 주면 아래 기본 문구가 나감.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없어요"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없어요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했어요");

    private final HttpStatus status;
    private final String message;
}
