package com.likelion.notebook.guide;

import com.likelion.notebook.guide.entity.Event;
import com.likelion.notebook.guide.entity.Event.EventType;
import com.likelion.notebook.guide.repository.EventRepository;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 일정 삭제 (DELETE /api/admin/events/{eventId}). local 오늘 = 2026-10-27 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventDeleteTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;

    @Test
    void 지우면_목록과_일지_기본_시점에_바로_반영() throws Exception {
        User admin = user("ev-admin", UserType.ADMIN, 47);
        User member = user("ev-member", UserType.MEMBER, 47);
        event(47, "시작", EventType.PROJECT_START, "2026-10-20");
        Event hackathon = event(47, "해커톤", EventType.HACKATHON, "2026-10-30"); // 3일 뒤 → DURING

        mvc.perform(get("/api/journals/form").header("X-User-Id", member.getId()))
                .andExpect(jsonPath("$.data.defaultTiming").value("DURING"));

        deleteEvent(admin, hackathon).andExpect(status().isOk());

        assertThat(eventRepository.findById(hackathon.getId())).isEmpty();
        mvc.perform(get("/api/admin/events").header("X-User-Id", admin.getId()))
                .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get("/api/journals/form").header("X-User-Id", member.getId()))
                .andExpect(jsonPath("$.data.defaultTiming").value("AFTER")); // 남은 큰 행사 없음
    }

    @Test
    void 다른_기수_404_아기사자_403() throws Exception {
        User admin = user("ev-admin2", UserType.ADMIN, 48);
        User member = user("ev-member2", UserType.MEMBER, 48);
        Event otherCohort = event(49, "다른 기수", EventType.MIDTERM, "2026-11-10");
        Event mine = event(48, "중간", EventType.MIDTERM, "2026-11-10");

        deleteEvent(admin, otherCohort)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("일정을 찾을 수 없어요"));
        deleteEvent(member, mine).andExpect(status().isForbidden());
        assertThat(eventRepository.findById(mine.getId())).isPresent();
    }

    private ResultActions deleteEvent(User user, Event e) throws Exception {
        return mvc.perform(delete("/api/admin/events/" + e.getId()).header("X-User-Id", user.getId()));
    }

    private Event event(int cohort, String name, EventType type, String date) {
        return eventRepository.save(Event.builder().cohort(cohort).name(name).type(type)
                .eventDate(LocalDate.parse(date)).build());
    }

    private User user(String sub, UserType type, int cohort) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("이름").type(type).cohort(cohort).build());
    }
}
