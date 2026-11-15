package com.likelion.notebook.guide.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.guide.dto.admin.EventCreateRequest;
import com.likelion.notebook.guide.dto.admin.EventResponse;
import com.likelion.notebook.guide.entity.Event;
import com.likelion.notebook.guide.entity.Event.EventType;
import com.likelion.notebook.guide.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** FN-ADM-04 일정. 저장하는 순간 모든 홈의 주차·D-day에 반영 (홈이 매번 계산하므로) */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> list(Integer cohort) {
        return eventRepository.findByCohortOrderByEventDateAsc(cohort).stream().map(EventResponse::from).toList();
    }

    @Transactional
    public EventResponse create(Integer cohort, EventCreateRequest request) {
        if (request.type() == EventType.PROJECT_START
                && eventRepository.existsByCohortAndType(cohort, EventType.PROJECT_START)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "프로젝트 시작 일정은 기수마다 하나예요");
        }
        Event saved = eventRepository.save(Event.builder()
                .cohort(cohort)
                .name(request.name().strip())
                .type(request.type())
                .eventDate(request.eventDate())
                .build());
        return EventResponse.from(saved);
    }

    /**
     * 잘못 넣은 일정 지우기. 본인 기수만 (다른 기수·없음은 404).
     * 홈 주차·D-day, 일지 기본 시점은 매번 계산하므로 바로 반영. 이미 쓴 일지의 timing은 그대로
     */
    @Transactional
    public void delete(Integer cohort, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getCohort().equals(cohort))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "일정을 찾을 수 없어요"));
        eventRepository.delete(event);
    }
}
