package com.likelion.notebook.journal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.user.entity.Part;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 7. 내 일지 상세 (GET /api/journals/{id}). 작성자 본인만 → 원문 + 정리본 전부, projectId도 줌.
 * 정리 전이면 summary = null
 */
public record JournalDetailResponse(
        Long id,
        Status status,
        Long projectId,
        Part part,
        Activity activity,
        Timing timing,
        Scope scope,
        Body raw,
        Summary summary,
        String rejectReason,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt
) {

    public record Body(String stuck, String solved, String again, String memo) {

        static Body of(JournalBody body) {
            return body == null ? new Body(null, null, null, null)
                    : new Body(body.getStuck(), body.getSolved(), body.getAgain(), body.getMemo());
        }
    }

    public record Summary(String title, List<Tag> tags, Body body) {
    }

    public static JournalDetailResponse of(Journal j) {
        Summary summary = j.isSummarized()
                ? new Summary(j.getSummaryTitle(), j.getTags(), Body.of(j.getSummary()))
                : null;
        return new JournalDetailResponse(
                j.getId(), j.getStatus(),
                j.getProject() == null ? null : j.getProject().getId(),
                j.getPart(), j.getActivity(), j.getTiming(), j.getScope(),
                Body.of(j.getRaw()), summary, j.getRejectReason(), j.getUpdatedAt());
    }
}
