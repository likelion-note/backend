package com.likelion.notebook.guide.dto;

import jakarta.validation.constraints.NotNull;

public record CheckRequest(@NotNull Boolean checked) {
}
