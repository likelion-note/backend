package com.likelion.notebook.journal.ai;

import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.user.entity.Part;

import java.util.List;

/**
 * AI 호출은 전부 이 인터페이스로만. 서비스는 어떤 AI인지 모름.
 * - AI_API_KEY(app.ai.api-key)가 있으면 GeminiAiClient, 비어 있으면 FakeAiClient (고정 응답)
 * 실패하면 AiClientException을 던짐 → 서비스가 500 INTERNAL_ERROR로 바꿈
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

    /**
     * 16. 포트폴리오 요약: 한 프로젝트의 내 일지 원문으로 이력서 문장.
     * 일지에 없는 숫자·성과는 지어내지 않고 ⟪숫자⟫·⟪결과⟫ 빈칸으로
     */
    String summarizePortfolio(PortfolioInput input);

    record PortfolioInput(
            String projectTitle,
            Part part,               // 가장 최근 일지의 파트
            PortfolioFormat format,
            List<PortfolioEntry> journals // 작성 순
    ) {
    }

    record PortfolioEntry(Activity activity, Timing timing, JournalBody raw) {
    }
}
