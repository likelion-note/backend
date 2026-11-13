package com.likelion.notebook.journal.dto;

/**
 * 14. 돌려보내기. reason 1~200자.
 * 빈 값·초과 검사는 Journal.reject에서 (문구: "사유를 200자 이내로 써 주세요")
 */
public record ReviewRejectRequest(String reason) {
}
