package com.likelion.notebook.journal;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 1. 작성 화면 기본값 (GET /api/journals/form)
 * local 프로필: app.today = 2026-10-27, 14기 demo 일정 → 시점 W1_2 (HomeGuideTest와 같은 기준)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalFormTest {

    private static final String URL = "/api/journals/form";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired AssignmentService assignmentService;

    @Test
    void 지금_배정이_기본값이고_지난_배정도_목록에_있다() throws Exception {
        User user = member("form-1", 14);
        ProjectCategory ideathon = team("14기 아이디어톤 · 6조");
        ProjectCategory hackathon = team("14기 해커톤 · 멋사수첩팀");
        assignmentService.assign(user.getId(), ideathon.getId(), Part.PM);
        assignmentService.assign(user.getId(), hackathon.getId(), Part.BACKEND); // 재배정 → 아이디어톤은 지난 배정

        mvc.perform(get(URL).header("X-User-Id", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projects.length()").value(2))
                .andExpect(jsonPath("$.data.projects[0].id").value(hackathon.getId()))
                .andExpect(jsonPath("$.data.projects[0].title").value("14기 해커톤 · 멋사수첩팀"))
                .andExpect(jsonPath("$.data.projects[0].part").value("BACKEND"))
                .andExpect(jsonPath("$.data.projects[1].id").value(ideathon.getId()))
                .andExpect(jsonPath("$.data.projects[1].part").value("PM"))
                .andExpect(jsonPath("$.data.defaultProjectId").value(hackathon.getId()))
                .andExpect(jsonPath("$.data.defaultPart").value("BACKEND"))
                .andExpect(jsonPath("$.data.defaultTiming").value("W1_2"));
    }

    @Test
    void 배정_전이면_미분류에_기본_파트() throws Exception {
        User user = member("form-2", 14);
        assignmentService.changeDefaultPart(user.getId(), Part.DESIGN);

        mvc.perform(get(URL).header("X-User-Id", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects").isEmpty())
                .andExpect(jsonPath("$.data.defaultProjectId").doesNotExist())
                .andExpect(jsonPath("$.data.defaultPart").value("DESIGN"))
                .andExpect(jsonPath("$.data.defaultTiming").value("W1_2"));
    }

    @Test
    void 일정이_없는_기수면_시점은_null() throws Exception {
        User user = member("form-3", 99);

        mvc.perform(get(URL).header("X-User-Id", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultTiming").doesNotExist());
    }

    @Test
    void 로그인_안_하면_401() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private User member(String sub, int cohort) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(cohort).build());
    }

    private ProjectCategory team(String title) {
        return projectRepository.save(ProjectCategory.builder()
                .title(title).kind(ProjectKind.TEAM).cohort(14).build());
    }
}
