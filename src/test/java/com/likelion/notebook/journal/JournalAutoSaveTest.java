package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.repository.JournalRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 3. 일지 저장 (PUT /api/journals/{id}) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalAutoSaveTest {

    private static final String STUCK_30 = "중간발표 2주 전에 주제를 바꿔야 할지 고민하다가 일주일을 그냥 보냈어요.";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired JournalRepository journalRepository;

    @Test
    void 보낸_값으로_덮어쓰고_안_보낸_칸은_비운다() throws Exception {
        User user = member("save-1");
        Journal journal = journalRepository.save(Journal.builder().author(user).scope(Scope.NOW)
                .raw(new JournalBody("처음 막힌 것", "처음 넘긴 방법", null, "메모")).build());

        putJson(user, journal.getId(), "{\"activity\": \"IDEATHON\", \"rawStuck\": \"고친 막힌 것\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(journal.getId()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        Journal saved = journalRepository.findById(journal.getId()).orElseThrow();
        assertThat(saved.getActivity().name()).isEqualTo("IDEATHON");
        assertThat(saved.getScope()).isNull();
        assertThat(saved.getRaw().getStuck()).isEqualTo("고친 막힌 것");
        assertThat(saved.getRaw().getSolved()).isNull();
        assertThat(saved.getRaw().getMemo()).isNull();
    }

    @Test
    void AI_정리본은_지우지_않는다() throws Exception {
        User user = member("save-2");
        Journal journal = journalRepository.save(Journal.builder().author(user)
                .raw(new JournalBody(STUCK_30, "넘긴 방법", null, null)).build());
        journal.applySummary("주제 변경을 미루지 않기", new JournalBody("정리된 막힌 것", "정리된 방법", null, null),
                List.of(Tag.TOPIC_CHANGE));

        putJson(user, journal.getId(), "{\"rawStuck\": \"%s 추가\"}".formatted(STUCK_30))
                .andExpect(status().isOk());

        Journal saved = journalRepository.findById(journal.getId()).orElseThrow();
        assertThat(saved.getSummaryTitle()).isEqualTo("주제 변경을 미루지 않기");
        assertThat(saved.getSummary().getStuck()).isEqualTo("정리된 막힌 것");
        assertThat(saved.getTags()).containsExactly(Tag.TOPIC_CHANGE);
    }

    @Test
    void 검토_요청한_일지는_수정할_수_없다() throws Exception {
        User user = member("save-3");
        Journal journal = journalRepository.save(Journal.builder().author(user).scope(Scope.NOW)
                .raw(new JournalBody(STUCK_30, "넘긴 방법", null, null)).build());
        journal.applySummary("제목", new JournalBody("정리", "정리", null, null), List.of());
        journal.submit(); // PENDING

        putJson(user, journal.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("지금은 수정할 수 없어요"));
    }

    @Test
    void 다른_사람_일지면_403() throws Exception {
        User owner = member("save-4");
        User other = member("save-5");
        Journal journal = journalRepository.save(Journal.builder().author(owner).build());

        putJson(other, journal.getId(), "{}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 일지만 수정할 수 있어요"));
    }

    @Test
    void 없는_일지면_404() throws Exception {
        putJson(member("save-6"), 999_999L, "{}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("일지를 찾을 수 없어요"));
    }

    @Test
    void id가_숫자가_아니면_400() throws Exception {
        mvc.perform(put("/api/journals/abc").header("X-User-Id", member("save-7").getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private ResultActions putJson(User user, Long journalId, String body) throws Exception {
        return mvc.perform(put("/api/journals/" + journalId).header("X-User-Id", user.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }
}
