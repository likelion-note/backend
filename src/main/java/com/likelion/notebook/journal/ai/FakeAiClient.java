package com.likelion.notebook.journal.ai;

import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 키 없이 쓰는 가짜 AI. 항상 같은 모양의 고정 응답.
 * - 제목·태그·보완 질문은 고정값
 * - 정리본은 원문을 그대로 돌려줌 (이름 가리기 안 함)
 * 실제 구현(Gemini)을 추가하면 AI_API_KEY가 없을 때만 이게 쓰이도록 조건을 붙이기
 */
@Component
public class FakeAiClient implements AiClient {

    public static final String TITLE = "[예시] 막혔던 순간과 넘긴 방법";
    public static final List<Tag> TAGS = List.of(Tag.ROLE_SPLIT);
    public static final String FOLLOW_UP = "그때 누구와 어떤 순서로 이야기했나요?";

    @Override
    public JournalSummary summarizeJournal(JournalInput input) {
        JournalBody raw = input.raw();
        JournalBody body = new JournalBody(raw.getStuck(), raw.getSolved(), raw.getAgain(), raw.getMemo());
        return new JournalSummary(TITLE, body, TAGS, input.askFollowUp() ? FOLLOW_UP : null);
    }
}
