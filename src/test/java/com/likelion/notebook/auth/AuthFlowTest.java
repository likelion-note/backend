package com.likelion.notebook.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** local 프로필(dev-mode) 기준: 가짜 구글 토큰 → 초대코드 가입 → JWT로 /api/me */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    @Test
    void 처음_로그인하면_가입하고_JWT로_내정보를_본다() throws Exception {
        // 1. 처음 로그인 → NEED_SIGNUP
        String first = mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"dev:sub-test-1:test1@gmail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NEED_SIGNUP"))
                .andReturn().getResponse().getContentAsString();
        String signupToken = om.readTree(first).at("/data/signupToken").asText();

        // 2. 틀린 초대코드 → 400
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(body(signupToken, "XXXXXX")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        // 3. 맞는 초대코드 → REGISTERED + accessToken
        String signed = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(body(signupToken, "LION14")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.user.cohort").value(14))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = om.readTree(signed).path("data");
        String accessToken = data.path("accessToken").asText();

        // 4. JWT로 /api/me
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("테스트"))
                .andExpect(jsonPath("$.data.type").value("MEMBER"));

        // 5. 같은 구글 계정으로 다시 로그인 → 바로 REGISTERED
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"dev:sub-test-1:test1@gmail.com\"}"))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"));

        // 6. 토큰 없이 → 401
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    private String body(String signupToken, String code) {
        return "{\"signupToken\":\"" + signupToken + "\",\"inviteCode\":\"" + code + "\",\"name\":\"테스트\"}";
    }
}
