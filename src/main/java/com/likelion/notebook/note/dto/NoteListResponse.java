package com.likelion.notebook.note.dto;

import java.util.List;

/** 8. 노트 목록 (page는 0부터) */
public record NoteListResponse(
        List<NoteCard> notes,
        int page,
        int size,
        long totalCount,
        boolean hasNext
) {
}
