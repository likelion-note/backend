package com.likelion.notebook.journal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 6. 내 기록 (GET /api/journals/me). 본인 화면이라 projectTitle을 줘도 됨 (미분류면 null)
 * title은 정리 전이면 null → 프론트에서 "제목 없음"
 */
public record MyJournalsResponse(Stats stats, List<Item> journals) {

    /** readCount·helpCount = 내 공개(APPROVED) 노트들의 note_reaction 합계 */
    public record Stats(long total, long approved, long pending, long rejected, long draft,
                        long readCount, long helpCount) {

        public static Stats of(List<Journal> journals, NoteImpact impact) {
            return new Stats(
                    journals.size(),
                    count(journals, Status.APPROVED),
                    count(journals, Status.PENDING),
                    count(journals, Status.REJECTED),
                    count(journals, Status.DRAFT),
                    impact.readCount(),
                    impact.helpCount());
        }

        private static long count(List<Journal> journals, Status status) {
            return journals.stream().filter(j -> j.getStatus() == status).count();
        }
    }

    public record Item(
            Long id,
            String title,
            Status status,
            Activity activity,
            Timing timing,
            String projectTitle,
            String rejectReason,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt
    ) {

        public static Item of(Journal j) {
            return new Item(j.getId(), j.getSummaryTitle(), j.getStatus(), j.getActivity(), j.getTiming(),
                    j.getProject() == null ? null : j.getProject().getTitle(),
                    j.getRejectReason(), j.getUpdatedAt());
        }
    }
}
