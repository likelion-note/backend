package com.likelion.notebook.journal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.service.AuthorLabels;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 11. 검토 목록 (운영진). 작성자 id·이름·팀 없음. authorLabel은 공개 범위와 상관없이 "14기 · PM"
 * pendingCount = pending 개수 (검토 탭 위 숫자)
 */
public record ReviewListResponse(int pendingCount, List<Pending> pending, List<Recent> recent) {

    public record Pending(
            Long journalId,
            String title,
            String authorLabel,
            Activity activity,
            Timing timing,
            Scope scope,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedAt // = updatedAt
    ) {

        public static Pending of(Journal j) {
            return new Pending(j.getId(), j.getSummaryTitle(), AuthorLabels.sameCohort(j),
                    j.getActivity(), j.getTiming(), j.getScope(), j.getUpdatedAt());
        }
    }

    public record Recent(
            Long journalId,
            String title,
            Status status,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime reviewedAt
    ) {

        public static Recent of(Journal j) {
            return new Recent(j.getId(), j.getSummaryTitle(), j.getStatus(), j.getReviewedAt());
        }
    }
}
