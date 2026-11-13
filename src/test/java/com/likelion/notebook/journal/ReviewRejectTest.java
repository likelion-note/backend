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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 14. 돌려보내기 (POST /api/admin/reviews/{journalId}/reject) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewRejectTest {

    @Autowired MockMvc mvc;
    @Autowired ReviewTestSupport s;
    @Autowired JournalRepository journalRepository;

    @Test
    void 돌려보내면_사유가_저장되고_작성자가_내_기록에서_본다() throws Exception {
        User admin = s.user("rr-admin", UserType.ADMIN, 61);
        User author = s.user("rr-author", UserType.MEMBER, 61);
        Journal j = s.pending(author, Scope.NOW, "돌려보낼 글");

        reject(admin, j, "{\"reason\": \"  팀 이름이 그대로 들어가 있어요  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        Journal saved = journalRepository.findById(j.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.REJECTED);
        assertThat(saved.getRejectReason()).isEqualTo("팀 이름이 그대로 들어가 있어요");
        assertThat(saved.getReviewer().getId()).isEqualTo(admin.getId());

        mvc.perform(get("/api/journals/" + j.getId()).header("X-User-Id", author.getId()))
                .andExpect(jsonPath("$.data.rejectReason").value("팀 이름이 그대로 들어가 있어요"));
    }

    @Test
    void 사유가_비거나_200자를_넘으면_400() throws Exception {
        User admin = s.user("rr-admin2", UserType.ADMIN, 62);
        Journal j = s.pending(s.user("rr-m", UserType.MEMBER, 62), Scope.NOW, "글");

        for (String body : new String[]{"{\"reason\": \"   \"}", "{}", "{\"reason\": \"%s\"}".formatted("가".repeat(201))}) {
            reject(admin, j, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("사유를 200자 이내로 써 주세요"));
        }
        reject(admin, j, "{\"reason\": \"%s\"}".formatted("가".repeat(200))).andExpect(status().isOk());
    }

    @Test
    void 이미_처리됨_400_본인_403_다른_기수_404() throws Exception {
        User admin = s.user("rr-admin3", UserType.ADMIN, 63);
        s.user("rr-admin4", UserType.ADMIN, 63);
        Journal j = s.pending(s.user("rr-m2", UserType.MEMBER, 63), Scope.NOW, "글");
        reject(admin, j, "{\"reason\": \"사유\"}").andExpect(status().isOk());
        reject(admin, j, "{\"reason\": \"사유\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 처리된 일지예요"));

        reject(admin, s.pending(admin, Scope.NOW, "본인 글"), "{\"reason\": \"사유\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 일지는 검토할 수 없어요"));

        reject(admin, s.pending(s.user("rr-m3", UserType.MEMBER, 64), Scope.NOW, "다른 기수"), "{\"reason\": \"사유\"}")
                .andExpect(status().isNotFound());
    }

    private ResultActions reject(User user, Journal j, String body) throws Exception {
        return mvc.perform(post("/api/admin/reviews/" + j.getId() + "/reject").header("X-User-Id", user.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
