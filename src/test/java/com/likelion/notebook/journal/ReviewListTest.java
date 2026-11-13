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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 11. 검토 목록 (GET /api/admin/reviews). 기수는 99기처럼 다른 테스트와 안 겹치게 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewListTest {

    @Autowired MockMvc mvc;
    @Autowired ReviewTestSupport s;
    @Autowired JournalRepository journalRepository;

    @Test
    void 같은_기수_대기열을_오래된_순으로_본인_일지는_빼고_보여준다() throws Exception {
        User admin = s.user("rl-admin1", UserType.ADMIN, 91);
        User admin2 = s.user("rl-admin2", UserType.ADMIN, 91);
        User m1 = s.user("rl-m1", UserType.MEMBER, 91);
        User otherCohort = s.user("rl-other", UserType.MEMBER, 92);

        Journal first = s.pending(m1, Scope.NOW, "먼저 요청");
        Thread.sleep(5);
        Journal second = s.pending(m1, Scope.NEXT, "나중 요청");
        s.pending(admin, Scope.NOW, "운영진 본인 글");     // 운영진 2명 → 본인 것은 빠짐
        s.pending(otherCohort, Scope.NOW, "다른 기수 글"); // 다른 기수 → 안 보임
        s.draft(m1);                                       // 임시 저장 → 안 보임

        mvc.perform(get("/api/admin/reviews").header("X-User-Id", admin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingCount").value(2))
                .andExpect(jsonPath("$.data.pending[*].journalId").value(contains(
                        first.getId().intValue(), second.getId().intValue())))
                .andExpect(jsonPath("$.data.pending[1].authorLabel").value("91기 · BACKEND")) // NEXT여도 팀 없이
                .andExpect(jsonPath("$.data.pending[1].scope").value("NEXT"))
                .andExpect(jsonPath("$.data.pending[0].requestedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.pending[0].author").doesNotExist())
                .andExpect(jsonPath("$.data.pending[0].projectId").doesNotExist());

        // 다른 운영진에게는 첫 운영진의 글이 보임
        mvc.perform(get("/api/admin/reviews").header("X-User-Id", admin2.getId()))
                .andExpect(jsonPath("$.data.pendingCount").value(3));
    }

    @Test
    void 운영진이_1명뿐이면_본인_일지도_대기열에_있다() throws Exception {
        User admin = s.user("rl-solo", UserType.ADMIN, 93);
        s.pending(admin, Scope.NOW, "혼자인 운영진 글");

        mvc.perform(get("/api/admin/reviews").header("X-User-Id", admin.getId()))
                .andExpect(jsonPath("$.data.pendingCount").value(1))
                .andExpect(jsonPath("$.data.pending[0].authorLabel").value("93기 운영진"));
    }

    @Test
    void 최근_처리한_것은_같은_기수_운영진_전체_기준_최신순() throws Exception {
        User admin = s.user("rl-admin3", UserType.ADMIN, 94);
        User admin2 = s.user("rl-admin4", UserType.ADMIN, 94);
        User m = s.user("rl-m2", UserType.MEMBER, 94);

        Journal approved = s.pending(m, Scope.NOW, "승인된 글");
        approved.approve(admin2);              // 다른 운영진이 처리
        journalRepository.flush();
        Thread.sleep(5);
        Journal rejected = s.pending(m, Scope.NOW, "돌려보낸 글");
        rejected.reject(admin, "팀 이름이 있어요");
        journalRepository.flush();

        mvc.perform(get("/api/admin/reviews").header("X-User-Id", admin.getId()))
                .andExpect(jsonPath("$.data.pendingCount").value(0))
                .andExpect(jsonPath("$.data.recent[*].journalId").value(contains(
                        rejected.getId().intValue(), approved.getId().intValue())))
                .andExpect(jsonPath("$.data.recent[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.data.recent[0].reviewedAt").isNotEmpty());
    }

    @Test
    void 아기사자는_403() throws Exception {
        User member = s.user("rl-m3", UserType.MEMBER, 95);
        mvc.perform(get("/api/admin/reviews").header("X-User-Id", member.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("운영진만 할 수 있어요"));
    }
}
