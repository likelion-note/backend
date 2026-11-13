package com.likelion.notebook.journal.dto;

import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;

import java.util.List;

/**
 * 4. AI 정리 응답
 * - body: 이름 가린 정리본
 * - authorLabel, audience: 공개되면 이렇게 보인다는 미리보기
 * - followUpQuestion: 처음 정리할 때만, 두 번째부터 null. DB에 저장 안 함
 */
public record JournalSummaryResponse(
        String title,
        List<Tag> tags,
        Body body,
        String authorLabel,
        String audience,
        String followUpQuestion
) {

    public record Body(String stuck, String solved, String again, String memo) {

        public static Body of(JournalBody body) {
            return new Body(body.getStuck(), body.getSolved(), body.getAgain(), body.getMemo());
        }
    }
}
