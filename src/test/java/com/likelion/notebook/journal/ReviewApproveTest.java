package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 13. 승인 (POST /api/admin/reviews/{journalId}/approve) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewApproveTest {

    @Autowired MockMvc mvc;
    @Autowired ReviewTestSupport s;
    @Autowired JournalRepository journalRepository;

    @Test
    void 승인하면_공개되고_검토자와_시각이_남고_노트_게시판에_보인다() throws Exception {
        User admin = s.user("ra-admin", UserType.ADMIN, 71);
        User author = s.user("ra-author", UserType.MEMBER, 71);
        User reader = s.user("ra-reader", UserType.MEMBER, 71);
        Journal j = s.pending(author, Scope.NOW, "승인할 글");

        approve(admin, j)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.journalId").value(j.getId()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        Journal saved = journalRepository.findById(j.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(saved.getReviewer().getId()).isEqualTo(admin.getId());
        assertThat(saved.getReviewedAt()).isNotNull();

        mvc.perform(get("/api/notes/" + j.getId()).header("X-User-Id", reader.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void 이미_처리된_일지는_400() throws Exception {
        User admin = s.user("ra-admin2", UserType.ADMIN, 72);
        Journal j = s.pending(s.user("ra-m", UserType.MEMBER, 72), Scope.NOW, "글");
        approve(admin, j).andExpect(status().isOk());

        approve(admin, j)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 처리된 일지예요"));
    }

    @Test
    void 본인_일지는_403_운영진이_1명이면_허용() throws Exception {
        User admin = s.user("ra-admin3", UserType.ADMIN, 73);
        Journal own = s.pending(admin, Scope.NOW, "혼자인 운영진 글");
        approve(admin, own).andExpect(status().isOk()); // 73기 운영진 1명

        s.user("ra-admin4", UserType.ADMIN, 73);         // 운영진 2명이 되면
        Journal own2 = s.pending(admin, Scope.NOW, "운영진 둘일 때 글");
        approve(admin, own2)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 일지는 검토할 수 없어요"));
    }

    @Test
    void 다른_기수_임시_저장은_404_아기사자는_403() throws Exception {
        User admin = s.user("ra-admin5", UserType.ADMIN, 74);
        approve(admin, s.pending(s.user("ra-m2", UserType.MEMBER, 75), Scope.NOW, "다른 기수"))
                .andExpect(status().isNotFound());
        approve(admin, s.draft(s.user("ra-m3", UserType.MEMBER, 74))).andExpect(status().isNotFound());

        User member = s.user("ra-m4", UserType.MEMBER, 74);
        approve(member, s.pending(s.user("ra-m5", UserType.MEMBER, 74), Scope.NOW, "글"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("운영진만 할 수 있어요"));
    }

    private ResultActions approve(User user, Journal j) throws Exception {
        return mvc.perform(post("/api/admin/reviews/" + j.getId() + "/approve").header("X-User-Id", user.getId()));
    }
}
