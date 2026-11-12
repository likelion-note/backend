package com.likelion.notebook.meeting;

import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** local 프로필: 오늘 = 2026-10-27 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeetingTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;

    @Test
    void 질문과_팁_템플릿() throws Exception {
        User me = member("mt-0");
        mvc.perform(get("/api/meetings/templates").header("X-User-Id", me.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.types", hasSize(4)))
                .andExpect(jsonPath("$.data.types[1].type").value("IDEA"))
                .andExpect(jsonPath("$.data.types[1].label").value("아이디어 회의"))
                .andExpect(jsonPath("$.data.types[1].questions", hasSize(3)))
                .andExpect(jsonPath("$.data.types[1].tips[0].tags[0].label").exists());
    }

    @Test
    void 저장하면_공유_원문이_나오고_본인만_본다() throws Exception {
        User me = member("mt-1");
        User other = member("mt-2");

        String body = """
                {"type":"IDEA","meetingDate":"2026-10-27",
                 "decisions":["주제는 동아리 일정 관리로", "  "],
                 "actions":[{"who":"민수","what":"경쟁 서비스 조사","due":"수"},{"who":"","what":"","due":""}]}
                """;
        String res = mvc.perform(post("/api/meetings").header("X-User-Id", me.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decisions", hasSize(1)))
                .andExpect(jsonPath("$.data.actions", hasSize(1)))
                .andExpect(jsonPath("$.data.shareText").value(
                        "아이디어 회의 · 10/27\n\n정한 것\n- 주제는 동아리 일정 관리로\n\n누가 언제까지\n- 민수 · 경쟁 서비스 조사 (수)"))
                .andReturn().getResponse().getContentAsString();
        String id = res.replaceAll(".*\"id\":(\\d+).*", "$1");

        // 지난 7일보다 오래된 회의
        mvc.perform(post("/api/meetings").header("X-User-Id", me.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"FIRST\",\"meetingDate\":\"2026-10-10\",\"decisions\":[\"주 2회 회의\"]}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/meetings").header("X-User-Id", me.getId()))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].meetingDate").value("2026-10-27"));
        mvc.perform(get("/api/meetings").param("days", "7").header("X-User-Id", me.getId()))
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 남의 회의는 없는 것처럼
        mvc.perform(get("/api/meetings/" + id).header("X-User-Id", other.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/meetings").header("X-User-Id", other.getId()))
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 삭제
        mvc.perform(delete("/api/meetings/" + id).header("X-User-Id", me.getId()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/meetings/" + id).header("X-User-Id", me.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 정한_것과_할_일이_둘_다_비면_400() throws Exception {
        User me = member("mt-3");
        mvc.perform(post("/api/meetings").header("X-User-Id", me.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RETRO\",\"decisions\":[\" \"],\"actions\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }
}
