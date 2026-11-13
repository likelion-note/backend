package com.likelion.notebook.journal;

import com.likelion.notebook.journal.dto.JournalBanner;
import com.likelion.notebook.journal.dto.NoteImpact;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.journal.service.JournalService;
import com.likelion.notebook.note.entity.NoteReaction;
import com.likelion.notebook.note.repository.NoteReactionRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 6. 내 기록 (GET /api/journals/me) + 홈용 journalBanner·noteImpact */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalMineTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired JournalRepository journalRepository;
    @Autowired NoteReactionRepository reactionRepository;
    @Autowired JournalService journalService;

    @Test
    void 통계와_목록을_최근_수정_순으로_준다() throws Exception {
        User me = member("mine-1");
        User admin = admin("mine-admin");
        ProjectCategory team = projectRepository.save(ProjectCategory.builder()
                .title("14기 해커톤 · 멋사수첩팀").kind(ProjectKind.TEAM).cohort(14).build());

        Journal draft = journalRepository.saveAndFlush(base(me).build());          // 정리 전, 미분류
        Journal approved = approved(me, team, admin);
        Journal rejected = summarized(me, null);
        rejected.submit(List.of());
        rejected.reject(admin, "팀 이름이 그대로 들어가 있어요");
        journalRepository.flush();
        Thread.sleep(5);
        draft.updateDraft(null, Part.PM, Activity.SESSION, Timing.W2PLUS, Scope.NOW,
                new JournalBody("다시 쓴 막힌 것", null, null, null));                // 가장 최근 수정
        journalRepository.flush();

        mvc.perform(get("/api/journals/me").header("X-User-Id", me.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.total").value(3))
                .andExpect(jsonPath("$.data.stats.approved").value(1))
                .andExpect(jsonPath("$.data.stats.pending").value(0))
                .andExpect(jsonPath("$.data.stats.rejected").value(1))
                .andExpect(jsonPath("$.data.stats.draft").value(1))
                .andExpect(jsonPath("$.data.journals.length()").value(3))
                .andExpect(jsonPath("$.data.journals[0].id").value(draft.getId()))
                .andExpect(jsonPath("$.data.journals[0].title").doesNotExist())
                .andExpect(jsonPath("$.data.journals[0].projectTitle").doesNotExist())
                .andExpect(jsonPath("$.data.journals[0].activity").value("SESSION"))
                .andExpect(jsonPath("$.data.journals[?(@.id == %d)].projectTitle".formatted(approved.getId()))
                        .value("14기 해커톤 · 멋사수첩팀"))
                .andExpect(jsonPath("$.data.journals[?(@.id == %d)].rejectReason".formatted(rejected.getId()))
                        .value("팀 이름이 그대로 들어가 있어요"));
    }

    @Test
    void 반응_합계는_내_공개_노트만_센다() throws Exception {
        User me = member("mine-2");
        User other = member("mine-3");
        User reader1 = member("mine-r1");
        User reader2 = member("mine-r2");
        User admin = admin("mine-admin2");

        Journal myApproved = approved(me, null, admin);
        Journal myPending = summarized(me, null);
        myPending.submit(List.of());
        Journal othersApproved = approved(other, null, admin);

        react(reader1, myApproved, true);
        react(reader2, myApproved, false);
        react(reader1, myPending, true);      // 공개 전 → 안 셈
        react(reader1, othersApproved, true); // 남의 노트 → 안 셈

        mvc.perform(get("/api/journals/me").header("X-User-Id", me.getId()))
                .andExpect(jsonPath("$.data.stats.readCount").value(2))
                .andExpect(jsonPath("$.data.stats.helpCount").value(1));

        assertThat(journalService.noteImpact(me.getId())).isEqualTo(new NoteImpact(2, 1));
        assertThat(journalService.journalBanner(me.getId())).isEqualTo(new JournalBanner(0, 1));
    }

    @Test
    void 일지가_없으면_전부_0() throws Exception {
        mvc.perform(get("/api/journals/me").header("X-User-Id", member("mine-4").getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.total").value(0))
                .andExpect(jsonPath("$.data.stats.readCount").value(0))
                .andExpect(jsonPath("$.data.journals").isEmpty());
    }

    @Test
    void 로그인_안_하면_401() throws Exception {
        mvc.perform(get("/api/journals/me")).andExpect(status().isUnauthorized());
    }

    private void react(User reader, Journal journal, boolean helped) {
        NoteReaction reaction = new NoteReaction(reader, journal);
        if (helped) {
            reaction.toggleHelp();
        }
        reactionRepository.save(reaction);
    }

    private Journal.JournalBuilder base(User user) {
        return Journal.builder().author(user).part(Part.BACKEND)
                .activity(Activity.HACKATHON).timing(Timing.W1_2).scope(Scope.NOW)
                .raw(new JournalBody(JournalSummaryTest.STUCK_30, "넘긴 방법", null, null));
    }

    private Journal summarized(User user, ProjectCategory project) {
        Journal journal = journalRepository.saveAndFlush(base(user).project(project).build());
        journal.applySummary("정리된 제목", new JournalBody("정리", "정리", null, null), List.of(Tag.DELAY));
        return journal;
    }

    private Journal approved(User user, ProjectCategory project, User admin) {
        Journal journal = summarized(user, project);
        journal.submit(List.of(Tag.DELAY));
        journal.approve(admin);
        return journal;
    }

    private User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }

    private User admin(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("운영").type(UserType.ADMIN).cohort(14).build());
    }
}
