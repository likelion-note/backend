package com.likelion.notebook.guide.dto.admin;

import com.likelion.notebook.guide.entity.Event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * type: PROJECT_START(주제 공개 = 1주차 기준, 여러 개 가능) | SESSION(정기 세션) |
 *       IDEATHON | HACKATHON | MIDTERM | DEMODAY | ETC (큰 행사 → D-day 대상)
 */
public record EventCreateRequest(@NotBlank @Size(max = 60) String name, @NotNull EventType type, @NotNull LocalDate eventDate) {
}
