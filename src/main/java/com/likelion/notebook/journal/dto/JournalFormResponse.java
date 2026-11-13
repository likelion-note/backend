package com.likelion.notebook.journal.dto;

import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectAssignment;

import java.util.List;

/**
 * 1. 작성 화면 기본값 (GET /api/journals/form)
 * - projects: 내가 배정된 적 있는 프로젝트 전부. 지금 배정이 맨 앞
 * - defaultProjectId: 지금 배정. 없으면 null(미분류)
 * - defaultPart: 지금 배정의 파트. 없으면 기본 파트(users.part)
 * - defaultTiming: 일정 기준 시점. 그 기수 일정이 없으면 null
 */
public record JournalFormResponse(
        List<ProjectOption> projects,
        Long defaultProjectId,
        Part defaultPart,
        Timing defaultTiming
) {

    public record ProjectOption(Long id, String title, Part part) {

        public static ProjectOption of(ProjectAssignment assignment) {
            return new ProjectOption(
                    assignment.getProject().getId(),
                    assignment.getProject().getTitle(),
                    assignment.getPart());
        }
    }
}
