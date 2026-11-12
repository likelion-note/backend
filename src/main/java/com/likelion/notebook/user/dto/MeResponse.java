package com.likelion.notebook.user.dto;

import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;

/** 본인 정보. 이름은 본인에게만 보이니 포함해도 됨 (노트·검토 DTO에는 절대 넣지 않기) */
public record MeResponse(Long id, String name, UserType type, Integer cohort, Part part, Project project) {

    public record Project(Long id, String title) {
    }

    public static MeResponse from(User user) {
        ProjectCategory p = user.getCurrentProject();
        return new MeResponse(user.getId(), user.getName(), user.getType(), user.getCohort(), user.getPart(),
                p == null ? null : new Project(p.getId(), p.getTitle()));
    }
}