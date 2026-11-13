package com.likelion.notebook.journal;

import com.likelion.notebook.journal.ai.FakeAiClient;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.PortfolioSummary;
import com.likelion.notebook.journal.repository.PortfolioSummaryRepository;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 16. 포트폴리오 요약 만들기 (POST /api/portfolio/summaries). AI는 Fake */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioSummaryTest {

    @Autowired MockMvc mvc;
    @Autowired PortfolioTestSupport s;
    @Autowired PortfolioSummaryRepository summaryRepository;

    @Test
    void 임시_저장_뺀_내_일지로_요약하고_저장한다() throws Exception {
        User me = s.member("ps-ok");
        ProjectCategory p = s.project("14기 해커톤 · 멋사수첩팀");
        Journal j1 = s.submitted(me, p, "첫 일지");
        Journal j2 = s.submitted(me, p, "돌려받은 것도 포함");
        j2.reject(s.member("ps-admin"), "사유");
        s.journal(me, p, "임시 저장은 빠짐");

        summarize(me, body(p.getId(), "RESUME_3LINES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.format").value("RESUME_3LINES"))
                .andExpect(jsonPath("$.data.result").value(FakeAiClient.RESUME_3LINES))
                .andExpect(jsonPath("$.data.sourceJournalIds.length()").value(2))
                .andExpect(jsonPath("$.data.sourceJournalIds[0]").value(j1.getId()))
                .andExpect(jsonPath("$.data.sourceJournalIds[1]").value(j2.getId()))
                .andExpect(jsonPath("$.data.remainingToday").value(4))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        List<PortfolioSummary> saved = summaryRepository.findAll().stream()
                .filter(x -> x.getUser().getId().equals(me.getId())).toList();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getProject().getId()).isEqualTo(p.getId());

        summarize(me, body(p.getId(), "EXPERIENCE_PARAGRAPH"))
                .andExpect(jsonPath("$.data.result").value(FakeAiClient.EXPERIENCE_PARAGRAPH))
                .andExpect(jsonPath("$.data.remainingToday").value(3));
    }

    @Test
    void 일지가_1개면_400() throws Exception {
        User me = s.member("ps-one");
        ProjectCategory p = s.project("팀");
        s.submitted(me, p, "하나뿐");
        s.journal(me, p, "임시 저장");

        summarize(me, body(p.getId(), "RESUME_3LINES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("일지가 2개 이상 있어야 해요"));
    }

    @Test
    void 입력이_틀리거나_내_프로젝트가_아니면_400() throws Exception {
        User me = s.member("ps-bad");
        User other = s.member("ps-other");
        ProjectCategory p = s.project("남의 팀");
        s.submitted(other, p, "남의 일지1");
        s.submitted(other, p, "남의 일지2");

        for (String body : new String[]{
                body(p.getId(), "RESUME_3LINES"),      // 내 일지 없음
                body(999999L, "RESUME_3LINES"),        // 없는 프로젝트
                "{\"format\": \"RESUME_3LINES\"}",
                "{\"projectId\": " + p.getId() + "}",
                body(p.getId(), "NOPE")}) {
            summarize(me, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요"));
        }
    }

    @Test
    void 하루_5번_넘으면_400() throws Exception {
        User me = s.member("ps-limit");
        ProjectCategory p = s.project("팀");
        s.submitted(me, p, "일지1");
        s.submitted(me, p, "일지2");

        for (int i = 4; i >= 0; i--) {
            summarize(me, body(p.getId(), "RESUME_3LINES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.remainingToday").value(i));
        }
        summarize(me, body(p.getId(), "RESUME_3LINES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘은 다 만들었어요. 내일 다시 해 주세요"));
    }

    @Test
    void 로그인_안_하면_401() throws Exception {
        mvc.perform(post("/api/portfolio/summaries").contentType(MediaType.APPLICATION_JSON)
                .content(body(1L, "RESUME_3LINES"))).andExpect(status().isUnauthorized());
    }

    private static String body(Long projectId, String format) {
        return "{\"projectId\": " + projectId + ", \"format\": \"" + format + "\"}";
    }

    private ResultActions summarize(User user, String body) throws Exception {
        return mvc.perform(post("/api/portfolio/summaries").header("X-User-Id", user.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
