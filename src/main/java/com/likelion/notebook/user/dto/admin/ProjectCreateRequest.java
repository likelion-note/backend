package com.likelion.notebook.user.dto.admin;

import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** SCR-ADM-06 분류 만들기: 제목 + 종류(TEAM 아기사자 팀 / OPS 운영) */
public record ProjectCreateRequest(@NotBlank @Size(max = 60) String title, @NotNull ProjectKind kind) {
}
