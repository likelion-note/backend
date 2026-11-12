package com.likelion.notebook.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 운영진 가입 → 프로젝트 만들기 → 아기사자 가입 → 기본 파트 → 배정 → 아기사자 홈에 할 일
 * (local: 오늘 2026-10-27, 2주차)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void 운영진이_배정하면_아기사자_홈에_할_일이_보인다() throws Exception {
        String admin = signup("dev:admin-flow:admin@test.com", "STAF14", "운영진");
        String member = signup("dev:member-flow:member@test.com", "LION14", "민수");
        long memberId = om.readTree(call(get("/api/me"), member)).at("/data/id").asLong();

        // 배정 전 홈: 할 일 없음
        mvc.perform(auth(get("/api/home"), member))
                .andExpect(jsonPath("$.data.assignment").doesNotExist())
                .andExpect(jsonPath("$.data.tasks", hasSize(0)));

        // 아기사자는 운영진 API 불가
        mvc.perform(auth(get("/api/admin/members"), member)).andExpect(status().isForbidden());

        // 프로젝트 만들기: 팀은 A, B 순서 / 운영은 글자 없음
        long teamId = om.readTree(call(post("/api/admin/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"14기 해커톤 · 멋사수첩팀\",\"kind\":\"TEAM\"}"), admin)).at("/data/id").asLong();
        mvc.perform(auth(post("/api/admin/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"14기 해커톤 · 두번째팀\",\"kind\":\"TEAM\"}"), admin))
                .andExpect(jsonPath("$.data.teamLetter").value("B"));
        mvc.perform(auth(post("/api/admin/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"14기 운영\",\"kind\":\"OPS\"}"), admin))
                .andExpect(jsonPath("$.data.teamLetter").doesNotExist());

        // 배정 전 그룹에 보임
        mvc.perform(auth(get("/api/admin/members"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unassigned[*].id", hasItem((int) memberId)));

        // 기본 파트 → 파트 없이 배정 → 기본 파트로 들어감
        mvc.perform(auth(put("/api/admin/members/" + memberId + "/default-part").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"part\":\"PM\"}"), admin))
                .andExpect(jsonPath("$.data.defaultPart").value("PM"));
        mvc.perform(auth(put("/api/admin/members/" + memberId + "/assignment").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + teamId + "}"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.part").value("PM"));

        // 멤버 화면: 배정 전 그룹에서 빠지고 팀의 PM 칸에
        mvc.perform(auth(get("/api/admin/members"), admin))
                .andExpect(jsonPath("$.data.unassigned[*].id", not(hasItem((int) memberId))))
                .andExpect(jsonPath("$.data.teams[0].membersByPart.PM[0].id").value(memberId));

        // 아기사자 홈: 배정 칩 + 2주차 PM 할 일
        mvc.perform(auth(get("/api/home"), member))
                .andExpect(jsonPath("$.data.assignment.projectTitle").value("14기 해커톤 · 멋사수첩팀"))
                .andExpect(jsonPath("$.data.assignment.part").value("PM"))
                .andExpect(jsonPath("$.data.tasks[0].key").value("PM-W2-0"));
    }

    @Test
    void 일정_등록과_프로젝트_시작_중복_방지() throws Exception {
        String admin = signup("dev:admin-event:admin2@test.com", "STAF14", "운영진2");

        mvc.perform(auth(post("/api/admin/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"아이디어톤\",\"type\":\"IDEATHON\",\"eventDate\":\"2026-11-01\"}"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.major").value(true));

        // local 데모 일정에 PROJECT_START가 이미 있음
        mvc.perform(auth(post("/api/admin/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"시작\",\"type\":\"PROJECT_START\",\"eventDate\":\"2026-10-20\"}"), admin))
                .andExpect(status().isBadRequest());

        mvc.perform(auth(get("/api/admin/events"), admin))
                .andExpect(jsonPath("$.data[*].name", hasItem("아이디어톤")));
    }

    private String signup(String idToken, String code, String name) throws Exception {
        JsonNode first = om.readTree(mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"" + idToken + "\"}")).andReturn().getResponse().getContentAsString());
        String signupToken = first.at("/data/signupToken").asText();
        JsonNode signed = om.readTree(mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"signupToken\":\"" + signupToken + "\",\"inviteCode\":\"" + code + "\",\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString());
        return signed.at("/data/accessToken").asText();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private String call(MockHttpServletRequestBuilder b, String token) throws Exception {
        return mvc.perform(auth(b, token)).andReturn().getResponse().getContentAsString();
    }
}
