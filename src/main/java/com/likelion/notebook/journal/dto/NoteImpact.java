package com.likelion.notebook.journal.dto;

/**
 * 내 공개(APPROVED) 노트의 읽음·도움돼요 합계.
 * 6. 내 기록의 stats와 홈(/api/home, 파트 A)의 noteImpact에서 같이 씀
 */
public record NoteImpact(long readCount, long helpCount) {
}
