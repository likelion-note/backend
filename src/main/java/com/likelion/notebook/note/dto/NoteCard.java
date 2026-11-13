package com.likelion.notebook.note.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;

import java.util.List;

/**
 * 노트 카드 (8번 목록, 9번 similarNotes, 파트 A 회의 도우미 "관련 노트"에서 같은 모양).
 * 익명성: 작성자 id·이름·projectId 없음. 작성자 표시는 authorLabel 하나
 */
public record NoteCard(
        Long id,
        String title,
        String authorLabel,
        @JsonProperty("isCurrentCohort") boolean currentCohort,
        @JsonProperty("isMine") boolean mine,
        Activity activity,
        Timing timing,
        List<Tag> tags,
        String preview,
        String previewFrom, // SOLVED | STUCK
        long readCount,
        long helpCount,
        @JsonProperty("isRead") boolean read
) {
}
