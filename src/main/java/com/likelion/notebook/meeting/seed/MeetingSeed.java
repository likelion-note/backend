package com.likelion.notebook.meeting.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.meeting.entity.Meeting.MeetingType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * seed/meeting-helper.json 을 앱 시작 시 한 번 읽어 메모리에 둠 (DB에 안 넣음).
 * 회의 종류 4개마다 오늘 정할 질문 3개(FN-MEET-01) + 막힐 때 팁(FN-MEET-02)
 */
@Component
@RequiredArgsConstructor
public class MeetingSeed {

    private final ObjectMapper objectMapper;

    private List<Template> templates;

    @PostConstruct
    void load() throws IOException {
        try (InputStream in = new ClassPathResource("seed/meeting-helper.json").getInputStream()) {
            this.templates = List.copyOf(objectMapper.readValue(in, new TypeReference<List<Template>>() {
            }));
        }
    }

    public List<Template> templates() {
        return templates;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Template(MeetingType type, List<Question> questions, List<Tip> tips) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Question(String question, String hint) {
    }

    /** tags: 관련 노트 2개를 찾을 때 쓰는 상황 태그 (노트 게시판 API의 tag 필터로) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tip(String title, List<String> steps, String source, List<Tag> tags) {
    }
}
