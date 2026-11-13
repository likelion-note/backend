package com.likelion.notebook.journal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.service.AuthorLabels;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 12. 검토 상세 (운영진). 공개될 정리본 그대로 — 원문 X, 작성자 id·이름·팀 X.
 * authorLabel은 공개 범위와 상관없이 "14기 · PM"
 */
public record ReviewDetailResponse(
        Long journalId,
        Status status,
        String title,
        List<Tag> tags,
        Body body,
        String authorLabel,
        Activity activity,
        Timing timing,
        Scope scope,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedAt
) {

    public record Body(String stuck, String solved, String again, String memo) {
    }

    public static ReviewDetailResponse of(Journal j) {
        JournalBody s = j.getSummary();
        Body body = s == null ? null : new Body(s.getStuck(), s.getSolved(), s.getAgain(), s.getMemo());
        return new ReviewDetailResponse(j.getId(), j.getStatus(), j.getSummaryTitle(), j.getTags(), body,
                AuthorLabels.sameCohort(j), j.getActivity(), j.getTiming(), j.getScope(), j.getUpdatedAt());
    }
}
