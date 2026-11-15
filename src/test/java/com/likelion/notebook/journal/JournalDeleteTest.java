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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 내 일지 삭제 (DELETE /api/journals/{id}): DRAFT·REJECTED만 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalDeleteTest {

    @Autowired MockMvc mvc;
    @Autowired ReviewTestSupport s;
    @Autowired JournalRepository journalRepository;

    @Test
    void 임시_저장과_돌려받은_일지는_지워지고_내_기록에서_빠진다() throws Exception {
        User me = s.user("jd-me", UserType.MEMBER, 71);
        User admin = s.user("jd-admin", UserType.ADMIN, 71);
        Journal draft = s.draft(me);
        Journal rejected = s.pending(me, Scope.NOW, "돌려받을 글");
        rejected.reject(admin, "사유");
        journalRepository.flush();

        deleteJournal(me, draft).andExpect(status().isOk());
        deleteJournal(me, rejected).andExpect(status().isOk());

        assertThat(journalRepository.findById(draft.getId())).isEmpty();
        assertThat(journalRepository.findById(rejected.getId())).isEmpty();
        mvc.perform(get("/api/journals/me").header("X-User-Id", me.getId()))
                .andExpect(jsonPath("$.data.stats.total").value(0));
    }

    @Test
    void 검토_중_공개됨은_400() throws Exception {
        User me = s.user("jd-me2", UserType.MEMBER, 72);
        User admin = s.user("jd-admin2", UserType.ADMIN, 72);
        Journal pending = s.pending(me, Scope.NOW, "검토 중");
        Journal approved = s.pending(me, Scope.NOW, "공개됨");
        approved.approve(admin);
        journalRepository.flush();

        deleteJournal(me, pending)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("검토 중인 일지는 지울 수 없어요"));
        deleteJournal(me, approved)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("공개된 노트는 지울 수 없어요"));
        assertThat(journalRepository.findById(pending.getId())).isPresent();
        assertThat(journalRepository.findById(approved.getId())).isPresent();
    }

    @Test
    void 남의_일지_403_없는_일지_404_로그인_안_하면_401() throws Exception {
        User me = s.user("jd-me3", UserType.MEMBER, 73);
        Journal others = s.draft(s.user("jd-other", UserType.MEMBER, 73));

        deleteJournal(me, others)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 일지만 지울 수 있어요"));
        mvc.perform(delete("/api/journals/999999").header("X-User-Id", me.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("일지를 찾을 수 없어요"));
        mvc.perform(delete("/api/journals/" + others.getId())).andExpect(status().isUnauthorized());
    }

    private ResultActions deleteJournal(User user, Journal j) throws Exception {
        return mvc.perform(delete("/api/journals/" + j.getId()).header("X-User-Id", user.getId()));
    }
}
