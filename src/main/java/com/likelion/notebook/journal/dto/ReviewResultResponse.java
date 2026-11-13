package com.likelion.notebook.journal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Status;

import java.time.LocalDateTime;

/** 13. 승인 / 14. 돌려보내기 결과 */
public record ReviewResultResponse(
        Long journalId,
        Status status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime reviewedAt
) {

    public static ReviewResultResponse of(Journal j) {
        return new ReviewResultResponse(j.getId(), j.getStatus(), j.getReviewedAt());
    }
}
