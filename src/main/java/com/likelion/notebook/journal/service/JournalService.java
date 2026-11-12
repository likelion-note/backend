package com.likelion.notebook.journal.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.guide.repository.EventRepository;
import com.likelion.notebook.guide.service.StageService;
import com.likelion.notebook.journal.dto.JournalFormResponse;
import com.likelion.notebook.journal.dto.JournalFormResponse.ProjectOption;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.user.entity.ProjectAssignment;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.ProjectAssignmentRepository;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final UserRepository userRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final EventRepository eventRepository;
    private final StageService stageService;

    /** 1. 작성 화면 기본값 */
    @Transactional(readOnly = true)
    public JournalFormResponse getForm(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<ProjectAssignment> assignments = findAllAssignments(userId);
        Optional<ProjectAssignment> current = assignments.stream()
                .filter(ProjectAssignment::isCurrent)
                .findFirst();

        return new JournalFormResponse(
                assignments.stream().map(ProjectOption::of).toList(),
                current.map(a -> a.getProject().getId()).orElse(null),
                current.map(ProjectAssignment::getPart).orElse(user.getPart()),
                defaultTiming(user.getCohort()));
    }

    /**
     * 내가 배정된 적 있는 배정 전부 (지난 배정·운영 분류 포함). 지금 배정이 맨 앞, 나머지는 최근 배정 순.
     * A의 Repository를 건드리지 않으려고 기존 메서드 두 개를 조합함. 한 사람 배정은 몇 개뿐이라 N번 조회해도 괜찮음.
     */
    private List<ProjectAssignment> findAllAssignments(Long userId) {
        return assignmentRepository.findProjectIdsByUserId(userId).stream()
                .map(projectId -> assignmentRepository.findByUserIdAndProjectId(userId, projectId))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(ProjectAssignment::isCurrent).reversed()
                        .thenComparing(ProjectAssignment::getId, Comparator.reverseOrder()))
                .toList();
    }

    /** 그 기수 일정이 아예 없으면 계산할 수 없어서 null. 있으면 StageService 값 (다 지났으면 AFTER) */
    private Timing defaultTiming(Integer cohort) {
        if (eventRepository.findByCohortOrderByEventDateAsc(cohort).isEmpty()) {
            return null;
        }
        return stageService.current(cohort).timing();
    }
}
