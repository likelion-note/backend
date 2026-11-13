package com.likelion.notebook.journal;

import com.likelion.notebook.journal.ai.AiClient;
import com.likelion.notebook.journal.ai.AiClient.JournalInput;
import com.likelion.notebook.journal.ai.AiClient.JournalSummary;
import com.likelion.notebook.journal.ai.FakeAiClient;
import com.likelion.notebook.journal.entity.JournalBody;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** 키 없이 FakeAiClient가 AiClient로 주입되고, 고정 응답을 주는지 */
@SpringBootTest
class FakeAiClientTest {

    @Autowired AiClient aiClient;

    @Test
    void 키_없으면_가짜_AI가_쓰인다() {
        assertThat(aiClient).isInstanceOf(FakeAiClient.class);
    }

    @Test
    void 처음_정리면_보완_질문이_있고_정리본은_원문_그대로() {
        JournalBody raw = new JournalBody("막힌 것", "넘긴 방법", null, "메모");
        JournalSummary result = aiClient.summarizeJournal(new JournalInput(null, null, null, raw, true));

        assertThat(result.title()).isEqualTo(FakeAiClient.TITLE);
        assertThat(result.tags()).isEqualTo(FakeAiClient.TAGS);
        assertThat(result.followUpQuestion()).isEqualTo(FakeAiClient.FOLLOW_UP);
        assertThat(result.body().getStuck()).isEqualTo("막힌 것");
        assertThat(result.body().getMemo()).isEqualTo("메모");
    }

    @Test
    void 다시_정리면_보완_질문은_null() {
        JournalBody raw = new JournalBody("막힌 것", "넘긴 방법", null, null);
        assertThat(aiClient.summarizeJournal(new JournalInput(null, null, null, raw, false)).followUpQuestion())
                .isNull();
    }
}
