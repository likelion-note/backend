package com.likelion.notebook.guide;

import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import com.likelion.notebook.user.repository.UserRepository;
import com.likelion.notebook.user.service.AssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * local 프로필: app.today = 2026-10-27, demo-data 일정
 * (프로젝트 시작 10/19, 정기 세션 10/29, 중간발표 11/08 ...)
 * → 2주차(W2), D-12 중간발표, 다음 세션 10/29, 시점 MID
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HomeGuideTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired AssignmentService assignmentService;

    @Test
    void 배정된_아기사자_홈() throws Exception {
        User user = member("home-1");
        ProjectCategory team = projectRepository.save(ProjectCategory.builder()
                .title("14기 해커톤 · 멋사수첩팀").kind(ProjectKind.TEAM).cohort(14).build());
        assignmentService.changeDefaultPart(user.getId(), Part.PM);
        assignmentService.assign(user.getId(), team.getId(), null);

        mvc.perform(get("/api/home").header("X-User-Id", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignment.part").value("PM"))
                .andExpect(jsonPath("$.data.stage.week").value(2))
                .andExpect(jsonPath("$.data.stage.stage").value("W2"))
                .andExpect(jsonPath("$.data.stage.timing").value("MID"))
                .andExpect(jsonPath("$.data.stage.dday.type").value("MIDTERM"))
                .andExpect(jsonPath("$.data.stage.dday.daysLeft").value(12))
                .andExpect(jsonPath("$.data.stage.nextSession.date").value("2026-10-29"))
                .andExpect(jsonPath("$.data.stage.inProjectPeriod").value(true))
                .andExpect(jsonPath("$.data.stageLabel").value("2주차"))
                .andExpect(jsonPath("$.data.tasks[0].key").value("PM-W2-0"))
                .andExpect(jsonPath("$.data.taskSummary.done").value(0));

        // 체크 → 홈에 반영
        mvc.perform(put("/api/guide/checks/PM-W2-0").header("X-User-Id", user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checked").value(true));
        mvc.perform(get("/api/home").header("X-User-Id", user.getId()))
                .andExpect(jsonPath("$.data.tasks[0].checked").value(true))
                .andExpect(jsonPath("$.data.taskSummary.done").value(1));

        // 체크 해제
        mvc.perform(put("/api/guide/checks/PM-W2-0").header("X-User-Id", user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checked\":false}"))
                .andExpect(jsonPath("$.data.checked").value(false));

        // 없는 할 일
        mvc.perform(put("/api/guide/checks/NOPE").header("X-User-Id", user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checked\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 배정_전이면_할_일이_비어_있다() throws Exception {
        User user = member("home-2");
        mvc.perform(get("/api/home").header("X-User-Id", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignment").doesNotExist())
                .andExpect(jsonPath("$.data.tasks").isEmpty())
                .andExpect(jsonPath("$.data.stage.week").value(2));
    }

    @Test
    void 운영진은_홈이_없다() throws Exception {
        User admin = userRepository.save(User.builder().email("admin@x.com").googleSub("home-admin")
                .name("운영").type(UserType.ADMIN).cohort(14).build());
        mvc.perform(get("/api/home").header("X-User-Id", admin.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void 파트_가이드는_다른_파트와_주차도_볼_수_있다() throws Exception {
        User user = member("guide-1");
        mvc.perform(get("/api/guide").header("X-User-Id", user.getId())
                        .param("part", "DESIGN").param("stage", "W3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.part").value("DESIGN"))
                .andExpect(jsonPath("$.data.stage").value("W3"))
                .andExpect(jsonPath("$.data.currentStage").value("W2"))
                .andExpect(jsonPath("$.data.stages.length()").value(6))
                .andExpect(jsonPath("$.data.tasks").isNotEmpty());
    }

    private User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }
}
