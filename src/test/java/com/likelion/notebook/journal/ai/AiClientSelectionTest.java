package com.likelion.notebook.journal.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** 키가 있으면 GeminiAiClient가 주입되는지 (호출은 안 함) */
@SpringBootTest(properties = "app.ai.api-key=dummy-key")
class AiClientSelectionTest {

    @Autowired AiClient aiClient;

    @Test
    void 키가_있으면_Gemini가_쓰인다() {
        assertThat(aiClient).isInstanceOf(GeminiAiClient.class);
    }
}
