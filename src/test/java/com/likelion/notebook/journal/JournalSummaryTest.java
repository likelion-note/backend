package com.likelion.notebook.journal;

import com.likelion.notebook.journal.ai.FakeAiClient;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import com.likelion.notebook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 4. AI 정리 (POST /api/journals/{id}/summary). AI는 FakeAiClient */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalSummaryTest {

    static final String STUCK_30 = "중간발표 2주 전에 주제를 바꿔야 할지 고민하다가 일주일을 그냥 보냈어요.";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired JournalRepository journalRepository;

    @Test
    void 정리본을_저장하고_미리보기와_보완_질문을_준다() throws Exception {
        User user = member("sum-1");
        Journal journal = ready(user, Scope.NOW, null);

        summarize(user, journal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(FakeAiClient.TITLE))
                .andExpect(jsonPath("$.data.tags[0]").value("ROLE_SPLIT"))
                .andExpect(jsonPath("$.data.body.stuck").value(STUCK_30))
                .andExpect(jsonPath("$.data.body.solved").value("한 장짜리 메모로 먼저 공유했어요"))
                .andExpect(jsonPath("$.data.authorLabel").value("14기 · BACKEND"))
                .andExpect(jsonPath("$.data.audience").value("지금 14기에게 (같은 팀원 제외)"))
                .andExpect(jsonPath("$.data.followUpQuestion").value(FakeAiClient.FOLLOW_UP));

        Journal saved = journalRepository.findById(journal.getId()).orElseThrow();
        assertThat(saved.getSummaryTitle()).isEqualTo(FakeAiClient.TITLE);
        assertThat(saved.getSummary().getStuck()).isEqualTo(STUCK_30);
    }

    @Test
    void 두_번째_정리부터는_보완_질문이_없다() throws Exception {
        User user = member("sum-2");
        Journal journal = ready(user, Scope.NOW, null);
        summarize(user, journal).andExpect(status().isOk());

        summarize(user, journal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followUpQuestion").doesNotExist());
    }

    @Test
    void 다음_기수_공개면_팀_가명이_보이고_미분류면_생략() throws Exception {
        User user = member("sum-3");
        ProjectCategory team = projectRepository.save(ProjectCategory.builder()
                .title("14기 해커톤 · 멋사수첩팀").kind(ProjectKind.TEAM).cohort(14).teamLetter("B").build());

        summarize(user, ready(user, Scope.NEXT, team))
                .andExpect(jsonPath("$.data.authorLabel").value("지난 기수 B팀 · BACKEND"))
                .andExpect(jsonPath("$.data.audience").value("다음 기수(15기)부터"));
        summarize(user, ready(user, Scope.NEXT, null))
                .andExpect(jsonPath("$.data.authorLabel").value("지난 기수 · BACKEND"));
    }

    @Test
    void 운영진_글과_파트_없는_글의_표시() throws Exception {
        User admin = userRepository.save(User.builder().email("sum-admin@x.com").googleSub("sum-admin")
                .name("운영").type(UserType.ADMIN).cohort(14).build());
        summarize(admin, ready(admin, Scope.NOW, null))
                .andExpect(jsonPath("$.data.authorLabel").value("14기 운영진"));

        User user = member("sum-4");
        Journal noPart = journalRepository.save(Journal.builder().author(user)
                .activity(Activity.HACKATHON).timing(Timing.W1_2).scope(Scope.NOW)
                .raw(new JournalBody(STUCK_30, "넘긴 방법", null, null)).build());
        summarize(user, noPart)
                .andExpect(jsonPath("$.data.authorLabel").value("14기"));
    }

    @Test
    void 필수_조건이_안_맞으면_400_상황별_문구() throws Exception {
        User user = member("sum-5");
        expect400(user, Journal.builder().author(user).raw(new JournalBody(STUCK_30, "방법", null, null)),
                "활동·시점·공개 범위를 골라 주세요");
        expect400(user, base(user).raw(new JournalBody("짧아요", "방법", null, null)),
                "막힌 것을 30자 이상 써 주세요");
        expect400(user, base(user).raw(new JournalBody(STUCK_30, "  ", null, null)),
                "넘긴 방법을 써 주세요");
    }

    @Test
    void 검토_요청한_일지는_정리할_수_없다() throws Exception {
        User user = member("sum-6");
        Journal journal = ready(user, Scope.NOW, null);
        summarize(user, journal).andExpect(status().isOk());
        journal.submit(journal.getTags()); // PENDING

        summarize(user, journal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("지금은 정리할 수 없어요"));
    }

    @Test
    void 남의_일지_403_없는_일지_404() throws Exception {
        User owner = member("sum-7");
        User other = member("sum-8");
        summarize(other, ready(owner, Scope.NOW, null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 일지만 정리할 수 있어요"));
        mvc.perform(post("/api/journals/999999/summary").header("X-User-Id", other.getId()))
                .andExpect(status().isNotFound());
    }

    private void expect400(User user, Journal.JournalBuilder builder, String message) throws Exception {
        summarize(user, journalRepository.save(builder.build()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(message));
    }

    private Journal.JournalBuilder base(User user) {
        return Journal.builder().author(user).part(Part.BACKEND)
                .activity(Activity.HACKATHON).timing(Timing.W1_2).scope(Scope.NOW);
    }

    private Journal ready(User user, Scope scope, ProjectCategory project) {
        return journalRepository.save(base(user).scope(scope).project(project)
                .raw(new JournalBody(STUCK_30, "한 장짜리 메모로 먼저 공유했어요", null, null)).build());
    }

    private ResultActions summarize(User user, Journal journal) throws Exception {
        return mvc.perform(post("/api/journals/" + journal.getId() + "/summary").header("X-User-Id", user.getId()));
    }

    private User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }
}
