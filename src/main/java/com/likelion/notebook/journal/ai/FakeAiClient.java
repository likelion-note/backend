package com.likelion.notebook.journal.ai;

import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 키 없이 쓰는 가짜 AI. app.ai.api-key(AI_API_KEY)가 비어 있을 때만 쓰임 (있으면 GeminiAiClient).
 * - 제목·태그·보완 질문·포트폴리오 요약은 고정값
 * - 정리본은 원문을 그대로 돌려줌 (이름 가리기 안 함) → 실제 사용자 데이터에는 쓰지 않기
 * 테스트는 build.gradle에서 AI_API_KEY를 비워서 항상 이걸 씀
 */
@Component
@ConditionalOnExpression("'${app.ai.api-key:}'.isBlank()")
public class FakeAiClient implements AiClient {

    public static final String TITLE = "[예시] 막혔던 순간과 넘긴 방법";
    public static final List<Tag> TAGS = List.of(Tag.ROLE_SPLIT);
    public static final String FOLLOW_UP = "그때 누구와 어떤 순서로 이야기했나요?";
    public static final String RESUME_3LINES = """
            - [예시] 프로젝트에서 ⟪역할⟫을 맡아 막힌 문제를 정리하고 해결 방법을 팀에 공유
            - [예시] ⟪숫자⟫번의 회의로 역할 분담을 정해 일정 안에 ⟪결과⟫ 완성
            - [예시] 다시 한다면 할 일을 정리해 다음 프로젝트에 적용""";
    public static final String EXPERIENCE_PARAGRAPH =
            "[예시] 프로젝트에서 ⟪역할⟫을 맡았습니다. 진행 중 막힌 순간마다 원인을 적고 넘긴 방법을 정리했습니다. "
                    + "그 결과 ⟪결과⟫를 만들었고, 이 경험으로 문제를 기록하며 푸는 습관을 얻었습니다.";

    @Override
    public JournalSummary summarizeJournal(JournalInput input) {
        JournalBody raw = input.raw();
        JournalBody body = new JournalBody(raw.getStuck(), raw.getSolved(), raw.getAgain(), raw.getMemo());
        return new JournalSummary(TITLE, body, TAGS, input.askFollowUp() ? FOLLOW_UP : null);
    }

    @Override
    public String summarizePortfolio(PortfolioInput input) {
        return input.format() == PortfolioFormat.RESUME_3LINES ? RESUME_3LINES : EXPERIENCE_PARAGRAPH;
    }
}
