package com.likelion.notebook.user.dto.admin;

import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectAssignment;

public record AssignmentResponse(Long userId, Long projectId, String projectTitle, Part part) {

    public static AssignmentResponse from(ProjectAssignment a) {
        return new AssignmentResponse(a.getUser().getId(), a.getProject().getId(), a.getProject().getTitle(), a.getPart());
    }
}
