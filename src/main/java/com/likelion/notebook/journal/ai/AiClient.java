package com.likelion.notebook.journal.ai;

import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.user.entity.Part;

import java.util.List;

/**
 * AI 호출은 전부 이 인터페이스로만. 서비스는 어떤 AI인지 모름.
 * - 지금: FakeAiClient (고정 응답, 키 없이 동작)
 * - 나중: Gemini 구현 추가. 키는 AI_API_KEY 환경변수. 구현을 추가하면 키가 있을 때만 그쪽이 쓰이게 하기
 * 실패하면 AiClientException을 던짐 → 서비스가 500 INTERNAL_ERROR로 바꿈
 * 포트폴리오 요약(16번)은 그 API 만들 때 메서드 추가
 */
public interface AiClient {

    /** 4. AI 정리: 이름 가리기 + 제목 + 태그(최대 3개, 12개 목록 안) + (요청 시) 보완 질문 1개 */
    JournalSummary summarizeJournal(JournalInput input);

    record JournalInput(
            Part part,
            Activity activity,
            Timing timing,
            JournalBody raw,
            boolean askFollowUp // 처음 정리할 때만 true
    ) {
    }

    record JournalSummary(
            String title,
            JournalBody body,       // 이름 가린 정리본
            List<Tag> tags,
            String followUpQuestion // askFollowUp=false면 null
    ) {
    }
}
