package com.likelion.notebook.note.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;

import java.util.List;

/**
 * 9. 노트 상세. 정리본 전체 + 비슷한 노트(카드) 최대 2개.
 * 익명성: 작성자 id·이름·projectId 없음
 */
public record NoteDetailResponse(
        Long id,
        String title,
        String authorLabel,
        @JsonProperty("isCurrentCohort") boolean currentCohort,
        @JsonProperty("isMine") boolean mine,
        Activity activity,
        Timing timing,
        List<Tag> tags,
        Body body,
        long readCount,
        long helpCount,
        boolean helped,
        List<NoteCard> similarNotes
) {

    public record Body(String stuck, String solved, String again, String memo) {
    }
}
