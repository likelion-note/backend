package com.likelion.notebook.journal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Status;

import java.time.LocalDateTime;

/** 2. 일지 새로 만들기 / 3. 자동 저장 응답 */
public record JournalSaveResponse(
        Long id,
        Status status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt
) {

    public static JournalSaveResponse of(Journal journal) {
        return new JournalSaveResponse(journal.getId(), journal.getStatus(), journal.getUpdatedAt());
    }
}
