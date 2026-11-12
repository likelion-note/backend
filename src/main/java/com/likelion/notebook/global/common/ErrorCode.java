package com.likelion.notebook.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드는 여기 한 곳에 모아서 관리. 기능 추가할 때 자기 영역 칸에 추가하기.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없어요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했어요"),

    // auth / user (A)
    INVITE_CODE_INVALID(HttpStatus.BAD_REQUEST, "코드를 다시 확인해 주세요"),
    INVITE_CODE_DISABLED(HttpStatus.BAD_REQUEST, "사용이 끝난 코드예요"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없어요"),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트 분류를 찾을 수 없어요"),

    // meeting / guide (A)
    MEETING_EMPTY(HttpStatus.BAD_REQUEST, "정한 것이나 할 일 중 하나는 적어주세요"),
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "회의 기록을 찾을 수 없어요"),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없어요"),

    // journal / note (B)
    JOURNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "일지를 찾을 수 없어요"),
    JOURNAL_STUCK_TOO_SHORT(HttpStatus.BAD_REQUEST, "막힌 것을 30자 이상 적어 주세요"),
    JOURNAL_INVALID_STATUS(HttpStatus.BAD_REQUEST, "지금 상태에서는 할 수 없는 작업이에요"),
    REVIEW_OWN_JOURNAL(HttpStatus.FORBIDDEN, "본인 일지는 다른 운영진이 검토해요"),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "사유를 한 줄 적어 주세요"),
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "노트를 찾을 수 없어요"),
    AI_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI 정리에 실패했어요. 원문은 저장됐어요"),
    PORTFOLIO_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "오늘은 다 만들었어요. 내일 다시 만들 수 있어요"),
    PORTFOLIO_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "기록이 2개 이상 쌓이면 요약할 수 있어요");

    private final HttpStatus status;
    private final String message;
}
