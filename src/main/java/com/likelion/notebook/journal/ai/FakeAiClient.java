package com.likelion.notebook.journal.ai;

import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 키 없이 쓰는 가짜 AI. app.ai.api-key(AI_API_KEY)가 비어 있을 때만 쓰임 (있으면 GeminiAiClient).
 * - 제목·태그·보완 질문은 고정값
 * - 정리본은 원문을 그대로 돌려줌 (이름 가리기 안 함) → 실제 사용자 데이터에는 쓰지 않기
 * 테스트는 build.gradle에서 AI_API_KEY를 비워서 항상 이걸 씀
 */
@Component
@ConditionalOnExpression("'${app.ai.api-key:}'.isBlank()")
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
