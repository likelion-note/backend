package com.likelion.notebook.user.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.journal.repository.PortfolioSummaryRepository;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FN-ADM-02 프로젝트 분류. 운영진 본인 기수에 만들어짐.
 * 아기사자 팀(TEAM)은 만들 때 teamLetter를 A, B, C… 순서로 자동 부여 (운영진이 입력하지 않음)
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectCategoryRepository projectRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final JournalRepository journalRepository;
    private final PortfolioSummaryRepository portfolioSummaryRepository;

    @Transactional
    public ProjectResponse create(Integer cohort, ProjectCreateRequest request) {
        String letter = request.kind() == ProjectKind.TEAM ? nextTeamLetter(cohort) : null;
        ProjectCategory saved = projectRepository.save(ProjectCategory.builder()
                .title(request.title().strip())
                .kind(request.kind())
                .cohort(cohort)
                .teamLetter(letter)
                .build());
        return ProjectResponse.of(saved, 0);
    }

    /**
     * 잘못 만든 분류 지우기. 그 분류로 쓴 일지(임시 저장 포함)나 포트폴리오 요약이 있으면 막음
     * (노트 팀원 숨김·팀 가명이 이 분류에 기대기 때문). 배정은 이력까지 같이 지움 → 지금 배정이던 사람은 배정 전으로
     */
    @Transactional
    public void delete(Integer cohort, Long projectId) {
        ProjectCategory project = projectRepository.findById(projectId)
                .filter(p -> p.getCohort().equals(cohort))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프로젝트 분류를 찾을 수 없어요"));
        if (journalRepository.existsByProjectId(projectId) || portfolioSummaryRepository.existsByProjectId(projectId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "일지가 있는 분류는 지울 수 없어요");
        }
        assignmentRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }

    /** 기수 안에서 아직 안 쓴 첫 글자 (지운 팀 글자는 다시 씀 → 중복 없음) */
    private String nextTeamLetter(Integer cohort) {
        Set<String> used = projectRepository.findByCohortOrderByIdAsc(cohort).stream()
                .map(ProjectCategory::getTeamLetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (char c = 'A'; c <= 'Z'; c++) {
            if (!used.contains(String.valueOf(c))) {
                return String.valueOf(c);
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "팀은 26개까지 만들 수 있어요");
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(Integer cohort) {
        return projectRepository.findByCohortOrderByIdAsc(cohort).stream()
                .map(p -> ProjectResponse.of(p, assignmentRepository.findCurrentMembers(p.getId()).size()))
                .toList();
    }
}
