package com.likelion.notebook.user.dto.admin;

import com.likelion.notebook.user.entity.Part;
import jakarta.validation.constraints.NotNull;

/** SCR-ADM-05 배정. part를 비우면 기본 파트가 들어감. 운영 분류면 무조건 OPS */
public record AssignRequest(@NotNull Long projectId, Part part) {
}
