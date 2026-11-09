package com.likelion.notebook.journal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 일지 본문 4칸. 원문과 AI 정리본(이름 가린 것)에 같은 모양으로 씀 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class JournalBody {

    @Column(columnDefinition = "TEXT")
    private String stuck;  // 막힌 것 (30자 이상)

    @Column(columnDefinition = "TEXT")
    private String solved; // 어떻게 넘겼나

    @Column(columnDefinition = "TEXT")
    private String again;  // 다시 한다면 (선택)

    @Column(columnDefinition = "TEXT")
    private String memo;   // 자유 메모 (선택)
}
