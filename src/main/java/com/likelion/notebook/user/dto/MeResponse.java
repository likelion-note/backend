package com.likelion.notebook.user.dto;

import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectAssignment;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;

/**
 * 본인 정보. 이름은 본인에게만 보이니 포함해도 됨 (노트·검토 DTO에는 절대 넣지 않기)
 * - defaultPart: 운영진이 정한 기본 파트
 * - part, project: 지금 배정. 배정 전이면 null
 */
public record MeResponse(Long id, String name, UserType type, Integer cohort,
                         Part defaultPart, Part part, Project project) {

    public record Project(Long id, String title) {
    }

    public static MeResponse of(User user, ProjectAssignment current) {
        return new MeResponse(user.getId(), user.getName(), user.getType(), user.getCohort(),
                user.getPart(),
                current == null ? null : current.getPart(),
                current == null ? null : new Project(current.getProject().getId(), current.getProject().getTitle()));
    }
}
