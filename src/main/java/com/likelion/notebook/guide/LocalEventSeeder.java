package com.likelion.notebook.guide;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.notebook.guide.entity.Event;
import com.likelion.notebook.guide.entity.Event.EventType;
import com.likelion.notebook.guide.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;

/** local 프로필에서만: demo-data.json 의 일정을 DB에 넣음 (홈 주차·D-day 확인용) */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalEventSeeder implements CommandLineRunner {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (eventRepository.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource("seed/demo-data.json").getInputStream()) {
            JsonNode events = objectMapper.readTree(in).path("events");
            for (JsonNode e : events) {
                eventRepository.save(Event.builder()
                        .cohort(e.get("cohort").asInt())
                        .name(e.get("name").asText())
                        .type(EventType.valueOf(e.get("type").asText()))
                        .eventDate(LocalDate.parse(e.get("date").asText()))
                        .build());
            }
            log.info("[local] 일정 {}개 넣음", events.size());
        }
    }
}
