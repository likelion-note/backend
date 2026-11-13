package com.likelion.notebook.journal.dto;

/**
 * 홈(/api/home, 파트 A)의 journalBanner: 돌려받은(REJECTED)·검토 중(PENDING) 내 일지 개수.
 * 파트 A의 HomeService가 JournalService.journalBanner(userId)를 호출
 */
public record JournalBanner(long rejected, long pending) {
}
