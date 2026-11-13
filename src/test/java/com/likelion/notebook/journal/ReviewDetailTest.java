package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 12. 검토 상세 (GET /api/admin/reviews/{journalId}) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewDetailTest {

    @Autowired MockMvc mvc;
    @Autowired ReviewTestSupport s;
    @Autowired JournalRepository journalRepository;

    @Test
    void 공개될_정리본만_보여주고_원문과_작성자는_없다() throws Exception {
        User admin = s.user("rd-admin", UserType.ADMIN, 81);
        Journal j = s.pending(s.user("rd-m", UserType.MEMBER, 81), Scope.NEXT, "첫 회의에서 역할이 안 정해졌을 때");

        open(admin, j)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.journalId").value(j.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.title").value("첫 회의에서 역할이 안 정해졌을 때"))
                .andExpect(jsonPath("$.data.tags[0]").value("MEETING_RUN"))
                .andExpect(jsonPath("$.data.body.solved").value("정리된 방법"))
                .andExpect(jsonPath("$.data.authorLabel").value("81기 · BACKEND"))
                .andExpect(jsonPath("$.data.scope").value("NEXT"))
                .andExpect(jsonPath("$.data.requestedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.raw").doesNotExist())
                .andExpect(jsonPath("$.data.author").doesNotExist())
                .andExpect(jsonPath("$.data.projectId").doesNotExist());
    }

    @Test
    void 처리된_일지도_볼_수_있다() throws Exception {
        User admin = s.user("rd-admin2", UserType.ADMIN, 82);
        Journal j = s.pending(s.user("rd-m2", UserType.MEMBER, 82), Scope.NOW, "승인된 글");
        j.approve(admin);
        journalRepository.flush();

        open(admin, j).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void 임시_저장_다른_기수_없는_일지는_404_아기사자는_403() throws Exception {
        User admin = s.user("rd-admin3", UserType.ADMIN, 83);
        Journal draft = s.draft(s.user("rd-m3", UserType.MEMBER, 83));
        Journal otherCohort = s.pending(s.user("rd-m4", UserType.MEMBER, 84), Scope.NOW, "다른 기수");

        open(admin, draft).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("일지를 찾을 수 없어요"));
        open(admin, otherCohort).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/reviews/999999").header("X-User-Id", admin.getId()))
                .andExpect(status().isNotFound());

        User member = s.user("rd-m5", UserType.MEMBER, 83);
        open(member, otherCohort).andExpect(status().isForbidden());
    }

    private ResultActions open(User user, Journal j) throws Exception {
        return mvc.perform(get("/api/admin/reviews/" + j.getId()).header("X-User-Id", user.getId()));
    }
}
