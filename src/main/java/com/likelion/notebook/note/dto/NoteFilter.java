package com.likelion.notebook.note.dto;

import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.user.entity.Part;

/** 8. 노트 목록 쿼리 파라미터 (값 목록에 없는 값은 400) */
public record NoteFilter(
        String q,
        CohortFilter cohort,
        Part part,
        Timing timing,
        Tag tag,
        NoteSort sort,
        int page,
        int size
) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    public enum CohortFilter {
        ALL,  // 기본
        PAST, // 지난 기수
        NOW   // 같은 기수 (내 노트 포함)
    }

    public enum NoteSort {
        NEW,   // 기본. 공개된 순(reviewedAt) 최신
        READS, // 많이 읽은 순
        HELPS  // 도움돼요 순
    }
}
