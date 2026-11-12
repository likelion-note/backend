package com.likelion.notebook.user.dto.admin;

import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;

/** teamLetter: 다음 기수에게 보이는 가명(A, B, C…). 운영 분류는 null */
public record ProjectResponse(Long id, String title, ProjectKind kind, Integer cohort, String teamLetter, int memberCount) {

    public static ProjectResponse of(ProjectCategory p, int memberCount) {
        return new ProjectResponse(p.getId(), p.getTitle(), p.getKind(), p.getCohort(), p.getTeamLetter(), memberCount);
    }
}
