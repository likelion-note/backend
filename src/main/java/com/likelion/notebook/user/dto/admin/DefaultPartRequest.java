package com.likelion.notebook.user.dto.admin;

import com.likelion.notebook.user.entity.Part;
import jakarta.validation.constraints.NotNull;

public record DefaultPartRequest(@NotNull Part part) {
}
