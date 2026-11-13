package com.likelion.notebook.journal;

import com.likelion.notebook.journal.ai.AiClient;
import com.likelion.notebook.journal.ai.AiClientException;
import com.likelion.notebook.journal.repository.PortfolioSummaryRepository;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 16. AI가 실패하면 500이고 횟수는 그대로 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioSummaryAiFailTest {

    @Autowired MockMvc mvc;
    @Autowired PortfolioTestSupport s;
    @Autowired PortfolioSummaryRepository summaryRepository;

    @MockitoBean AiClient aiClient;

    @Test
    void AI가_실패하면_500이고_횟수가_줄지_않는다() throws Exception {
        given(aiClient.summarizePortfolio(any())).willThrow(new AiClientException("timeout", null));
        User me = s.member("ps-fail");
        ProjectCategory p = s.project("팀");
        s.submitted(me, p, "일지1");
        s.submitted(me, p, "일지2");
        long before = summaryRepository.count();

        mvc.perform(post("/api/portfolio/summaries").header("X-User-Id", me.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\": " + p.getId() + ", \"format\": \"RESUME_3LINES\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("요약에 실패했어요. 잠시 후 다시 시도해 주세요"));

        assertThat(summaryRepository.count()).isEqualTo(before);
        mvc.perform(get("/api/portfolio/projects").header("X-User-Id", me.getId()))
                .andExpect(jsonPath("$.data.remainingToday").value(5));
    }
}
