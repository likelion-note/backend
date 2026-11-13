package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 7. 내 일지 상세 (GET /api/journals/{id}) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalDetailTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired JournalRepository journalRepository;

    @Test
    void 돌려받은_일지는_원문_정리본_사유를_모두_준다() throws Exception {
        User me = member("detail-1");
        User admin = userRepository.save(User.builder().email("detail-admin@x.com").googleSub("detail-admin")
                .name("운영").type(UserType.ADMIN).cohort(14).build());
        ProjectCategory team = projectRepository.save(ProjectCategory.builder()
                .title("14기 해커톤 · 멋사수첩팀").kind(ProjectKind.TEAM).cohort(14).build());
        Journal journal = journalRepository.save(Journal.builder().author(me).project(team).part(Part.BACKEND)
                .activity(Activity.HACKATHON).timing(Timing.W1_2).scope(Scope.NOW)
                .raw(new JournalBody(JournalSummaryTest.STUCK_30, "원문 넘긴 방법", "다시 한다면", null)).build());
        journal.applySummary("정리된 제목", new JournalBody("정리된 막힌 것", "정리된 방법", null, null),
                List.of(Tag.MEETING_RUN));
        journal.submit(List.of(Tag.MEETING_RUN));
        journal.reject(admin, "팀 이름이 그대로 들어가 있어요");
        journalRepository.flush();

        mvc.perform(get("/api/journals/" + journal.getId()).header("X-User-Id", me.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(journal.getId()))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.projectId").value(team.getId()))
                .andExpect(jsonPath("$.data.part").value("BACKEND"))
                .andExpect(jsonPath("$.data.scope").value("NOW"))
                .andExpect(jsonPath("$.data.raw.stuck").value(JournalSummaryTest.STUCK_30))
                .andExpect(jsonPath("$.data.raw.again").value("다시 한다면"))
                .andExpect(jsonPath("$.data.raw.memo").doesNotExist())
                .andExpect(jsonPath("$.data.summary.title").value("정리된 제목"))
                .andExpect(jsonPath("$.data.summary.tags[0]").value("MEETING_RUN"))
                .andExpect(jsonPath("$.data.summary.body.solved").value("정리된 방법"))
                .andExpect(jsonPath("$.data.rejectReason").value("팀 이름이 그대로 들어가 있어요"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    @Test
    void 정리_전이면_summary는_null() throws Exception {
        User me = member("detail-2");
        Journal journal = journalRepository.save(Journal.builder().author(me).build());

        mvc.perform(get("/api/journals/" + journal.getId()).header("X-User-Id", me.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.projectId").doesNotExist())
                .andExpect(jsonPath("$.data.summary").doesNotExist())
                .andExpect(jsonPath("$.data.raw").exists());
    }

    @Test
    void 남의_일지_403_없는_일지_404() throws Exception {
        Journal journal = journalRepository.save(Journal.builder().author(member("detail-3")).build());
        User other = member("detail-4");

        mvc.perform(get("/api/journals/" + journal.getId()).header("X-User-Id", other.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 일지만 볼 수 있어요"));
        mvc.perform(get("/api/journals/999999").header("X-User-Id", other.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("일지를 찾을 수 없어요"));
    }

    @Test
    void me와_form은_상세로_잡히지_않는다() throws Exception {
        User me = member("detail-5");
        mvc.perform(get("/api/journals/me").header("X-User-Id", me.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats").exists());
        mvc.perform(get("/api/journals/form").header("X-User-Id", me.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects").exists());
    }

    private User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }
}
