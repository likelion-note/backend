package com.likelion.notebook.journal.dto;

import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.user.entity.Part;
import jakarta.validation.constraints.Size;

/**
 * 2. 일지 새로 만들기 / 3. 자동 저장 공통 body. 임시 저장이라 전부 선택.
 * 값 목록에 없는 enum 값은 JSON 변환에서 막힘 → 400 INVALID_INPUT
 */
public record JournalSaveRequest(
        Long projectId, // null = 미분류
        Part part,
        Activity activity,
        Timing timing,
        Scope scope,
        @Size(max = RAW_MAX, message = "입력값을 확인해 주세요") String rawStuck,
        @Size(max = RAW_MAX, message = "입력값을 확인해 주세요") String rawSolved,
        @Size(max = RAW_MAX, message = "입력값을 확인해 주세요") String rawAgain,
        @Size(max = RAW_MAX, message = "입력값을 확인해 주세요") String rawMemo
) {

    public static final int RAW_MAX = 2000;

    public JournalBody toRaw() {
        return new JournalBody(rawStuck, rawSolved, rawAgain, rawMemo);
    }
}
