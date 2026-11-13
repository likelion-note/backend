package com.likelion.notebook.journal;

import com.likelion.notebook.journal.ai.AiClient;
import com.likelion.notebook.journal.ai.AiClientException;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 4. AI 정리: AI가 실패하면 500, 원문은 그대로 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalSummaryAiFailTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired JournalRepository journalRepository;

    @MockitoBean AiClient aiClient;

    @Test
    void AI가_실패하면_500이고_원문은_남는다() throws Exception {
        given(aiClient.summarizeJournal(any())).willThrow(new AiClientException("timeout", null));

        User user = userRepository.save(User.builder().email("fail@x.com").googleSub("sum-fail")
                .name("민수").type(UserType.MEMBER).cohort(14).build());
        Journal journal = journalRepository.save(Journal.builder().author(user)
                .activity(Activity.HACKATHON).timing(Timing.W1_2).scope(Scope.NOW)
                .raw(new JournalBody(JournalSummaryTest.STUCK_30, "넘긴 방법", null, null)).build());

        mvc.perform(post("/api/journals/" + journal.getId() + "/summary").header("X-User-Id", user.getId()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("정리에 실패했어요. 잠시 후 다시 시도해 주세요"));

        Journal saved = journalRepository.findById(journal.getId()).orElseThrow();
        assertThat(saved.getRaw().getStuck()).isEqualTo(JournalSummaryTest.STUCK_30);
        assertThat(saved.getSummaryTitle()).isNull();
    }
}
