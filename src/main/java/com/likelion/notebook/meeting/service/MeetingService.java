package com.likelion.notebook.meeting.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.meeting.dto.MeetingCreateRequest;
import com.likelion.notebook.meeting.dto.MeetingResponse;
import com.likelion.notebook.meeting.dto.MeetingTemplateResponse;
import com.likelion.notebook.meeting.entity.Meeting;
import com.likelion.notebook.meeting.entity.MeetingAction;
import com.likelion.notebook.meeting.repository.MeetingRepository;
import com.likelion.notebook.meeting.seed.MeetingSeed;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * 회의 정리는 작성자 본인만 봄 (남의 회의는 NOT_FOUND로 존재 자체를 숨김).
 * 노트로 올라가지 않음. B의 성장일지 "회의 불러오기"는 list(userId, 7)을 쓰면 됨.
 */
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingSeed meetingSeed;
    private final Clock clock;

    public MeetingTemplateResponse templates() {
        return MeetingTemplateResponse.of(meetingSeed.templates());
    }

    @Transactional
    public MeetingResponse create(Long userId, MeetingCreateRequest request) {
        List<String> decisions = request.decisions() == null ? List.of()
                : request.decisions().stream()
                .filter(d -> d != null && !d.isBlank())
                .map(String::strip)
                .toList();
        List<MeetingAction> actions = request.actions() == null ? List.of()
                : request.actions().stream()
                .filter(a -> a != null && a.what() != null && !a.what().isBlank())
                .map(a -> new MeetingAction(strip(a.who()), a.what().strip(), strip(a.due())))
                .toList();

        if (decisions.isEmpty() && actions.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "정한 것이나 할 일을 하나 이상 적어 주세요");
        }

        Meeting meeting = meetingRepository.save(Meeting.builder()
                .owner(userRepository.getReferenceById(userId))
                .type(request.type())
                .meetingDate(request.meetingDate() != null ? request.meetingDate() : LocalDate.now(clock))
                .decisions(decisions)
                .actions(actions)
                .build());
        return MeetingResponse.from(meeting);
    }

    /** days를 주면 오늘 포함 최근 N일 (days=7 → 오늘-6일 ~ 오늘). 최신순 */
    @Transactional(readOnly = true)
    public List<MeetingResponse> list(Long userId, Integer days) {
        List<Meeting> meetings;
        if (days == null) {
            meetings = meetingRepository.findByOwnerIdOrderByMeetingDateDescIdDesc(userId);
        } else {
            if (days < 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "days는 1 이상이에요");
            }
            LocalDate today = LocalDate.now(clock);
            meetings = meetingRepository.findByOwnerIdAndMeetingDateBetweenOrderByMeetingDateDescIdDesc(
                    userId, today.minusDays(days - 1L), today);
        }
        return meetings.stream().map(MeetingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(Long userId, Long meetingId) {
        return MeetingResponse.from(findMine(userId, meetingId));
    }

    @Transactional
    public void delete(Long userId, Long meetingId) {
        meetingRepository.delete(findMine(userId, meetingId));
    }

    private Meeting findMine(Long userId, Long meetingId) {
        return meetingRepository.findById(meetingId)
                .filter(m -> m.getOwner().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회의 정리를 찾을 수 없어요"));
    }

    private static String strip(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
