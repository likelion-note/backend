package com.likelion.notebook.journal.dto;

import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 5. 검토 요청. tags = AI가 준 태그 중 남길 것 (빼기만 가능, 빈 배열 가능) */
public record JournalSubmitRequest(
        @NotNull(message = "입력값을 확인해 주세요") List<Tag> tags
) {
}
