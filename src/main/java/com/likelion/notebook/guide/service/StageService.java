package com.likelion.notebook.guide.service;

import com.likelion.notebook.guide.entity.Event;
import com.likelion.notebook.guide.entity.Event.EventType;
import com.likelion.notebook.guide.repository.EventRepository;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * FN-MATCH-01 현재 단계 계산. 기수의 일정(events)과 오늘 날짜로 계산하고 저장하지 않음.
 * - 주차: PROJECT_START 일정 기준. 시작일이 1주차
 * - 주차 구간(stage): 1→W1, 2→W2, 3→W3, 4·5→W4_5, 6→W6, 7 이상→W7
 * - D-day: 오늘 이후 가장 가까운 "큰 행사"(PROJECT_START, SESSION 제외)
 * - 다음 정기 세션: D-day와 별도로 표시
 * - 시점(timing): 다음 큰 행사까지 14일 이상 W2PLUS, 7~13일 W1_2, 0~6일 DURING, 남은 큰 행사가 없으면 AFTER
 * - 프로젝트 기간: 시작일이 지났고 남은 큰 행사가 있을 때
 * B의 일지 작성 기본값(시점)도 이 서비스를 쓰면 됨.
 */
@Service
@RequiredArgsConstructor
public class StageService {

    private final EventRepository eventRepository;
    private final Clock clock;

    public record StageInfo(
            Integer week,          // 시작 전이거나 시작일 없으면 null
            String stage,          // W1 ... W7, week가 null이면 null
            Timing timing,
            Dday dday,             // 남은 큰 행사가 없으면 null
            NextSession nextSession,
            boolean inProjectPeriod
    ) {
    }

    public record Dday(String name, EventType type, LocalDate date, long daysLeft) {
    }

    public record NextSession(String name, LocalDate date) {
    }

    @Transactional(readOnly = true)
    public StageInfo current(Integer cohort) {
        LocalDate today = LocalDate.now(clock);
        List<Event> events = eventRepository.findByCohortOrderByEventDateAsc(cohort);

        Optional<LocalDate> start = events.stream()
                .filter(e -> e.getType() == EventType.PROJECT_START)
                .map(Event::getEventDate)
                .findFirst();

        Integer week = start
                .filter(s -> !today.isBefore(s))
                .map(s -> (int) (ChronoUnit.DAYS.between(s, today) / 7) + 1)
                .orElse(null);

        Optional<Event> nextMajor = events.stream()
                .filter(Event::isMajor)
                .filter(e -> !e.getEventDate().isBefore(today))
                .findFirst();

        Dday dday = nextMajor
                .map(e -> new Dday(e.getName(), e.getType(), e.getEventDate(),
                        ChronoUnit.DAYS.between(today, e.getEventDate())))
                .orElse(null);

        NextSession nextSession = events.stream()
                .filter(e -> e.getType() == EventType.SESSION)
                .filter(e -> !e.getEventDate().isBefore(today))
                .findFirst()
                .map(e -> new NextSession(e.getName(), e.getEventDate()))
                .orElse(null);

        boolean inPeriod = week != null && nextMajor.isPresent();

        return new StageInfo(week, stageOf(week), timingOf(dday), dday, nextSession, inPeriod);
    }

    static String stageOf(Integer week) {
        if (week == null) {
            return null;
        }
        if (week <= 1) return "W1";
        if (week == 2) return "W2";
        if (week == 3) return "W3";
        if (week <= 5) return "W4_5";
        if (week == 6) return "W6";
        return "W7";
    }

    static Timing timingOf(Dday dday) {
        if (dday == null) return Timing.AFTER;
        if (dday.daysLeft() >= 14) return Timing.W2PLUS;
        if (dday.daysLeft() >= 7) return Timing.W1_2;
        return Timing.DURING;
    }
}
