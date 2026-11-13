package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.UserRepository;
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

/** 5. 검토 요청 (POST /api/journals/{id}/submit) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalSubmitTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired JournalRepository journalRepository;

    @Test
    void 태그를_빼고_검토_요청한다() throws Exception {
        User user = member("sub-1");
        Journal journal = summarized(user, List.of(Tag.MEETING_RUN, Tag.ROLE_SPLIT));

        submit(user, journal, "{\"tags\": [\"MEETING_RUN\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(journal.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        Journal saved = journalRepository.findById(journal.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        assertThat(saved.getTags()).containsExactly(Tag.MEETING_RUN);
    }

    @Test
    void 태그를_전부_빼도_된다() throws Exception {
        User user = member("sub-2");
        Journal journal = summarized(user, List.of(Tag.DELAY));

        submit(user, journal, "{\"tags\": []}").andExpect(status().isOk());
        assertThat(journalRepository.findById(journal.getId()).orElseThrow().getTags()).isEmpty();
    }

    @Test
    void 새_태그를_추가하면_400() throws Exception {
        User user = member("sub-3");
        Journal journal = summarized(user, List.of(Tag.DELAY));

        submit(user, journal, "{\"tags\": [\"DELAY\", \"STACK\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("태그는 정리된 것 중에서만 고를 수 있어요"));
    }

    @Test
    void 정리본이_없으면_400() throws Exception {
        User user = member("sub-4");
        Journal journal = journalRepository.save(base(user).build());

        submit(user, journal, "{\"tags\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("먼저 AI 정리를 해 주세요"));
    }

    @Test
    void 이미_요청한_일지면_400() throws Exception {
        User user = member("sub-5");
        Journal journal = summarized(user, List.of(Tag.DELAY));
        submit(user, journal, "{\"tags\": [\"DELAY\"]}").andExpect(status().isOk());

        submit(user, journal, "{\"tags\": [\"DELAY\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 검토 요청한 일지예요"));
    }

    @Test
    void 돌려받은_일지를_다시_요청하면_사유를_지운다() throws Exception {
        User user = member("sub-6");
        User admin = userRepository.save(User.builder().email("sub-admin@x.com").googleSub("sub-admin")
                .name("운영").type(UserType.ADMIN).cohort(14).build());
        Journal journal = summarized(user, List.of(Tag.DELAY));
        journal.submit(List.of(Tag.DELAY));
        journal.reject(admin, "팀원 이름이 남아 있어요");

        submit(user, journal, "{\"tags\": [\"DELAY\"]}").andExpect(status().isOk());

        Journal saved = journalRepository.findById(journal.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        assertThat(saved.getRejectReason()).isNull();
    }

    @Test
    void tags가_없으면_400_남의_일지면_403() throws Exception {
        User owner = member("sub-7");
        Journal journal = summarized(owner, List.of(Tag.DELAY));

        submit(owner, journal, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        submit(member("sub-8"), journal, "{\"tags\": []}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 일지만 요청할 수 있어요"));
    }

    private Journal.JournalBuilder base(User user) {
        return Journal.builder().author(user).part(Part.PM)
                .activity(Activity.HACKATHON).timing(Timing.W1_2).scope(Scope.NOW)
                .raw(new JournalBody(JournalSummaryTest.STUCK_30, "넘긴 방법", null, null));
    }

    private Journal summarized(User user, List<Tag> tags) {
        Journal journal = journalRepository.save(base(user).build());
        journal.applySummary("제목", new JournalBody("정리된 막힌 것", "정리된 방법", null, null), tags);
        return journal;
    }

    private ResultActions submit(User user, Journal journal, String body) throws Exception {
        return mvc.perform(post("/api/journals/" + journal.getId() + "/submit").header("X-User-Id", user.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }
}
