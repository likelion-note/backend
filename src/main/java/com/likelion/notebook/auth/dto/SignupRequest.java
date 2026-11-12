package com.likelion.notebook.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String signupToken,
        @NotBlank @Size(min = 6, max = 6) String inviteCode,
        @NotBlank @Size(max = 30) String name
) {
}