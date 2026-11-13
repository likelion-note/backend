package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;
import com.likelion.notebook.journal.entity.PortfolioSummary;
import com.likelion.notebook.journal.repository.PortfolioSummaryRepository;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 15. 포트폴리오 프로젝트 목록 (GET /api/portfolio/projects) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioProjectsTest {

    @Autowired MockMvc mvc;
    @Autowired PortfolioTestSupport s;
    @Autowired PortfolioSummaryRepository summaryRepository;

    @Test
    void 일지_쓴_프로젝트만_개수와_함께_최근_수정_순으로() throws Exception {
        User me = s.member("pp-me");
        ProjectCategory old = s.project("13기 아이디어톤 · 옛팀");
        ProjectCategory recent = s.project("14기 해커톤 · 멋사수첩팀");
        ProjectCategory draftOnly = s.project("임시 저장만 있는 팀");

        s.submitted(me, old, "옛 일지1");
        s.submitted(me, old, "옛 일지2");
        s.journal(me, old, "옛 임시 저장");                // 임시 저장은 개수에서 빠짐
        Thread.sleep(5);
        s.submitted(me, recent, "최근 일지");              // 1개 → available false
        s.journal(me, draftOnly, "임시 저장만");           // 목록에서 빠짐
        s.submitted(me, null, "미분류");                   // 미분류 빠짐
        s.submitted(s.member("pp-other"), recent, "남의 일지");

        mvc.perform(get("/api/portfolio/projects").header("X-User-Id", me.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects.length()").value(2))
                .andExpect(jsonPath("$.data.projects[0].projectId").value(recent.getId()))
                .andExpect(jsonPath("$.data.projects[0].title").value("14기 해커톤 · 멋사수첩팀"))
                .andExpect(jsonPath("$.data.projects[0].journalCount").value(1))
                .andExpect(jsonPath("$.data.projects[0].available").value(false))
                .andExpect(jsonPath("$.data.projects[1].projectId").value(old.getId()))
                .andExpect(jsonPath("$.data.projects[1].journalCount").value(2))
                .andExpect(jsonPath("$.data.projects[1].available").value(true))
                .andExpect(jsonPath("$.data.remainingToday").value(5));
    }

    @Test
    void 오늘_만든_만큼_남은_횟수가_줄고_0_아래로_안_간다() throws Exception {
        User me = s.member("pp-count");
        ProjectCategory p = s.project("팀");
        for (int i = 0; i < 2; i++) {
            save(me, p);
        }
        mvc.perform(get("/api/portfolio/projects").header("X-User-Id", me.getId()))
                .andExpect(jsonPath("$.data.projects.length()").value(0))
                .andExpect(jsonPath("$.data.remainingToday").value(3));

        for (int i = 0; i < 4; i++) {
            save(me, p);
        }
        mvc.perform(get("/api/portfolio/projects").header("X-User-Id", me.getId()))
                .andExpect(jsonPath("$.data.remainingToday").value(0));
    }

    @Test
    void 로그인_안_하면_401() throws Exception {
        mvc.perform(get("/api/portfolio/projects")).andExpect(status().isUnauthorized());
    }

    private void save(User me, ProjectCategory p) {
        summaryRepository.saveAndFlush(PortfolioSummary.builder().user(me).project(p)
                .format(PortfolioFormat.RESUME_3LINES).result("요약").sourceJournalIds(List.of()).build());
    }
}
