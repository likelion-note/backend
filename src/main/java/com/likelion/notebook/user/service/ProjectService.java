package com.likelion.notebook.user.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.user.dto.admin.ProjectCreateRequest;
import com.likelion.notebook.user.dto.admin.ProjectResponse;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.repository.ProjectAssignmentRepository;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FN-ADM-02 프로젝트 분류. 운영진 본인 기수에 만들어짐.
 * 아기사자 팀(TEAM)은 만들 때 teamLetter를 A, B, C… 순서로 자동 부여 (운영진이 입력하지 않음)
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectCategoryRepository projectRepository;
    private final ProjectAssignmentRepository assignmentRepository;

    @Transactional
    public ProjectResponse create(Integer cohort, ProjectCreateRequest request) {
        String letter = null;
        if (request.kind() == ProjectKind.TEAM) {
            long teams = projectRepository.countByCohortAndKind(cohort, ProjectKind.TEAM);
            if (teams >= 26) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "팀은 26개까지 만들 수 있어요");
            }
            letter = String.valueOf((char) ('A' + teams));
        }
        ProjectCategory saved = projectRepository.save(ProjectCategory.builder()
                .title(request.title().strip())
                .kind(request.kind())
                .cohort(cohort)
                .teamLetter(letter)
                .build());
        return ProjectResponse.of(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(Integer cohort) {
        return projectRepository.findByCohortOrderByIdAsc(cohort).stream()
                .map(p -> ProjectResponse.of(p, assignmentRepository.findCurrentMembers(p.getId()).size()))
                .toList();
    }
}
