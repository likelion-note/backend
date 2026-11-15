package com.likelion.notebook.admin;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;
import com.likelion.notebook.journal.entity.PortfolioSummary;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.journal.repository.PortfolioSummaryRepository;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.ProjectAssignmentRepository;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import com.likelion.notebook.user.repository.UserRepository;
import com.likelion.notebook.user.service.AssignmentService;
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
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 분류 삭제 (DELETE /api/admin/projects/{projectId}) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminProjectDeleteTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired ProjectAssignmentRepository assignmentRepository;
    @Autowired AssignmentService assignmentService;
    @Autowired JournalRepository journalRepository;
    @Autowired PortfolioSummaryRepository summaryRepository;

    @Test
    void 빈_분류를_지우면_배정된_사람은_배정_전으로() throws Exception {
        User admin = user("pd-admin", UserType.ADMIN, 42);
        User member = user("pd-member", UserType.MEMBER, 42);
        ProjectCategory old = project("지난 팀", ProjectKind.TEAM, 42, "A");
        ProjectCategory wrong = project("잘못 만든 팀", ProjectKind.TEAM, 42, "B");
        assignmentService.assign(member.getId(), old.getId(), Part.PM);
        assignmentService.assign(member.getId(), wrong.getId(), Part.PM); // 지금 배정 = wrong, old는 이력

        deleteProject(admin, wrong).andExpect(status().isOk());

        assertThat(projectRepository.findById(wrong.getId())).isEmpty();
        assertThat(assignmentRepository.findByUserIdAndProjectId(member.getId(), wrong.getId())).isEmpty();
        assertThat(assignmentRepository.findByUserIdAndProjectId(member.getId(), old.getId())).isPresent(); // 다른 분류 이력은 그대로
        mvc.perform(get("/api/admin/members").header("X-User-Id", admin.getId()))
                .andExpect(jsonPath("$.data.unassigned[*].id", hasItem(member.getId().intValue())));
    }

    @Test
    void 일지나_포트폴리오_요약이_있으면_400() throws Exception {
        User admin = user("pd-admin2", UserType.ADMIN, 43);
        User member = user("pd-member2", UserType.MEMBER, 43);
        ProjectCategory withDraft = project("임시 저장 있는 팀", ProjectKind.TEAM, 43, "A");
        ProjectCategory withSummary = project("요약 있는 팀", ProjectKind.TEAM, 43, "B");
        journalRepository.save(Journal.builder().author(member).project(withDraft).build());
        summaryRepository.save(PortfolioSummary.builder().user(member).project(withSummary)
                .format(PortfolioFormat.RESUME_3LINES).result("요약").sourceJournalIds(List.of()).build());

        for (ProjectCategory p : List.of(withDraft, withSummary)) {
            deleteProject(admin, p)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("일지가 있는 분류는 지울 수 없어요"));
            assertThat(projectRepository.findById(p.getId())).isPresent();
        }
    }

    @Test
    void 다른_기수_404_아기사자_403() throws Exception {
        User admin = user("pd-admin3", UserType.ADMIN, 44);
        User member = user("pd-member3", UserType.MEMBER, 44);
        ProjectCategory otherCohort = project("다른 기수 팀", ProjectKind.TEAM, 45, "A");
        ProjectCategory mine = project("운영", ProjectKind.OPS, 44, null);

        deleteProject(admin, otherCohort)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("프로젝트 분류를 찾을 수 없어요"));
        deleteProject(member, mine).andExpect(status().isForbidden());
        deleteProject(admin, mine).andExpect(status().isOk()); // 운영 분류도 지울 수 있음
    }

    @Test
    void 지운_팀_글자는_다음에_다시_쓴다() throws Exception {
        User admin = user("pd-admin4", UserType.ADMIN, 46);
        create(admin, "팀1").andExpect(jsonPath("$.data.teamLetter").value("A"));
        create(admin, "팀2").andExpect(jsonPath("$.data.teamLetter").value("B"));
        create(admin, "팀3").andExpect(jsonPath("$.data.teamLetter").value("C"));

        ProjectCategory teamB = projectRepository.findByCohortOrderByIdAsc(46).stream()
                .filter(p -> "B".equals(p.getTeamLetter())).findFirst().orElseThrow();
        deleteProject(admin, teamB).andExpect(status().isOk());

        create(admin, "다시 만든 팀").andExpect(jsonPath("$.data.teamLetter").value("B"));
        create(admin, "네번째 팀").andExpect(jsonPath("$.data.teamLetter").value("D"));
    }

    private ResultActions deleteProject(User user, ProjectCategory p) throws Exception {
        return mvc.perform(delete("/api/admin/projects/" + p.getId()).header("X-User-Id", user.getId()));
    }

    private ResultActions create(User admin, String title) throws Exception {
        return mvc.perform(post("/api/admin/projects").header("X-User-Id", admin.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"" + title + "\", \"kind\": \"TEAM\"}"));
    }

    private User user(String sub, UserType type, int cohort) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("이름").type(type).cohort(cohort).build());
    }

    private ProjectCategory project(String title, ProjectKind kind, int cohort, String letter) {
        return projectRepository.save(ProjectCategory.builder().title(title).kind(kind).cohort(cohort).teamLetter(letter).build());
    }
}
