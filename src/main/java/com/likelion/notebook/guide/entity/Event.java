package com.likelion.notebook.guide.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 운영진이 등록하는 행사 일정. 바꾸면 모든 홈의 주차·D-day·할 일·추천이 바뀜.
 * 주차 = 프로젝트 시작일 기준, D-day = 정기 세션·프로젝트 시작을 뺀 "큰 행사" 기준
 */
@Getter
@Entity
@Table(name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer cohort;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Builder
    private Event(Integer cohort, String name, EventType type, LocalDate eventDate) {
        this.cohort = cohort;
        this.name = name;
        this.type = type;
        this.eventDate = eventDate;
    }

    public boolean isMajor() {
        return type != EventType.PROJECT_START && type != EventType.SESSION;
    }

    public enum EventType {
        PROJECT_START, SESSION, IDEATHON, HACKATHON, MIDTERM, DEMODAY, ETC
    }
}
