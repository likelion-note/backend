package com.likelion.notebook.admin;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 운영진도 멤버 화면 "배정 전"에 보이고, 거기서 운영 분류에 배정할 수 있음 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminMemberUnassignedTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;

    @Test
    void 배정_안_된_운영진이_배정_전에_보이고_운영_분류에_배정하면_운영_그룹으로() throws Exception {
        User admin = user("un-admin", UserType.ADMIN, 41);
        User other = user("un-admin2", UserType.ADMIN, 41);
        User member = user("un-member", UserType.MEMBER, 41);
        ProjectCategory ops = projectRepository.save(ProjectCategory.builder()
                .title("41기 운영진").kind(ProjectKind.OPS).cohort(41).build());

        mvc.perform(get("/api/admin/members").header("X-User-Id", admin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unassigned.length()").value(3))
                .andExpect(jsonPath("$.data.unassigned[?(@.id == %d)].type".formatted(other.getId())).value("ADMIN"))
                .andExpect(jsonPath("$.data.unassigned[?(@.id == %d)].type".formatted(member.getId())).value("MEMBER"));

        mvc.perform(put("/api/admin/members/" + other.getId() + "/assignment").header("X-User-Id", admin.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\": " + ops.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.part").value("OPS"));

        mvc.perform(get("/api/admin/members").header("X-User-Id", admin.getId()))
                .andExpect(jsonPath("$.data.unassigned[*].id", not(hasItem(other.getId().intValue()))))
                .andExpect(jsonPath("$.data.unassigned[*].id", hasItem(member.getId().intValue())))
                .andExpect(jsonPath("$.data.ops[0].members[0].id").value(other.getId()));
    }

    private User user(String sub, UserType type, int cohort) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("이름").type(type).cohort(cohort).build());
    }
}
