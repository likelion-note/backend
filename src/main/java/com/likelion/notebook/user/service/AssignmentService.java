package com.likelion.notebook.user.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectAssignment;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.ProjectAssignmentRepository;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 멤버 배정 규칙 (FN-ADM-03)
 * - 파트를 안 주면 기본 파트(users.part)를 복사
 * - 운영(OPS) 분류는 파트가 항상 OPS
 * - 재배정 시 이전 배정은 current=false로 남김. 이전 일지는 이전 분류에 그대로
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final UserRepository userRepository;
    private final ProjectCategoryRepository projectCategoryRepository;
    private final ProjectAssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public Optional<ProjectAssignment> findCurrent(Long userId) {
        return assignmentRepository.findCurrent(userId);
    }

    @Transactional
    public ProjectAssignment assign(Long userId, Long projectId, Part part) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        ProjectCategory project = projectCategoryRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Part resolved = resolvePart(user, project, part);

        Optional<ProjectAssignment> current = assignmentRepository.findCurrent(userId);
        if (current.isPresent() && current.get().getProject().getId().equals(projectId)) {
            current.get().changePart(resolved); // 같은 프로젝트면 파트만 바꿈
            return current.get();
        }
        current.ifPresent(ProjectAssignment::end);

        return assignmentRepository.findByUserIdAndProjectId(userId, projectId)
                .map(old -> {
                    old.reactivate(resolved);
                    return old;
                })
                .orElseGet(() -> assignmentRepository.save(new ProjectAssignment(user, project, resolved)));
    }

    /** 배정 해제 → 배정 전 상태로 */
    @Transactional
    public void unassign(Long userId) {
        assignmentRepository.findCurrent(userId).ifPresent(ProjectAssignment::end);
    }

    @Transactional
    public void changeDefaultPart(Long userId, Part part) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.changeDefaultPart(part);
    }

    private Part resolvePart(User user, ProjectCategory project, Part requested) {
        if (project.getKind() == ProjectKind.OPS) {
            return Part.OPS;
        }
        Part part = requested != null ? requested : user.getPart();
        if (part == null || part == Part.OPS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파트를 골라 주세요");
        }
        return part;
    }
}
