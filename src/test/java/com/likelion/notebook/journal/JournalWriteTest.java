package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.journal.repository.JournalRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 2. 일지 새로 만들기 (POST /api/journals) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalWriteTest {

    private static final String URL = "/api/journals";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired AssignmentService assignmentService;
    @Autowired JournalRepository journalRepository;

    @Test
    void 내_프로젝트로_임시_저장_일지를_만든다() throws Exception {
        User user = member("write-1");
        ProjectCategory team = team();
        assignmentService.assign(user.getId(), team.getId(), Part.BACKEND);

        String body = """
                {"projectId": %d, "part": "BACKEND", "activity": "HACKATHON", "timing": "W1_2", "scope": "NOW",
                 "rawStuck": "API 명세를 프론트랑 맞추다가 막혔어요", "rawSolved": "", "rawAgain": null, "rawMemo": ""}
                """.formatted(team.getId());

        String json = postJson(user, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.updatedAt").value(org.hamcrest.Matchers.matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.valueOf(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
        Journal saved = journalRepository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.DRAFT);
        assertThat(saved.getAuthor().getId()).isEqualTo(user.getId());
        assertThat(saved.getProject().getId()).isEqualTo(team.getId());
        assertThat(saved.getRaw().getStuck()).isEqualTo("API 명세를 프론트랑 맞추다가 막혔어요");
    }

    @Test
    void 빈_body면_미분류_빈_일지() throws Exception {
        postJson(member("write-2"), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void 내가_배정된_적_없는_프로젝트면_400() throws Exception {
        ProjectCategory other = team();
        postJson(member("write-3"), "{\"projectId\": %d}".formatted(other.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 값_목록에_없는_값이면_400() throws Exception {
        postJson(member("write-4"), "{\"part\": \"XYZ\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요"));
    }

    @Test
    void 글자_수_2000자_넘으면_400() throws Exception {
        postJson(member("write-5"), "{\"rawStuck\": \"%s\"}".formatted("가".repeat(2001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 로그인_안_하면_401() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions postJson(User user, String body) throws Exception {
        return mvc.perform(post(URL).header("X-User-Id", user.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }

    private ProjectCategory team() {
        return projectRepository.save(ProjectCategory.builder()
                .title("14기 해커톤 · 멋사수첩팀").kind(ProjectKind.TEAM).cohort(14).build());
    }
}
