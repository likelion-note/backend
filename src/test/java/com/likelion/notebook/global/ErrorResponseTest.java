package com.likelion.notebook.global;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 없는 API는 500이 아니라 404 "없는 API예요", 요청 형식 오류는 400 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorResponseTest {

    @Autowired MockMvc mvc;

    @Test
    void 없는_주소는_404() throws Exception {
        mvc.perform(get("/api/nonexistent-xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("없는 API예요"));
    }

    @Test
    void 주소는_있는데_메서드가_없으면_404() throws Exception {
        mvc.perform(delete("/api/health"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("없는 API예요"));
    }

    @Test
    void JSON이_아닌_body는_400() throws Exception {
        mvc.perform(post("/api/journals").header("X-User-Id", 1)
                        .contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
