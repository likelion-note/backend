package com.likelion.notebook.auth.controller;

import com.likelion.notebook.auth.dto.GoogleLoginRequest;
import com.likelion.notebook.auth.dto.InviteCodePreviewResponse;
import com.likelion.notebook.auth.dto.LoginResponse;
import com.likelion.notebook.auth.dto.SignupRequest;
import com.likelion.notebook.auth.service.AuthService;
import com.likelion.notebook.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 전에 부르는 API. 인증 헤더 필요 없음 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ApiResponse<LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.ok(authService.googleLogin(request));
    }

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @GetMapping("/invite-codes/{code}")
    public ApiResponse<InviteCodePreviewResponse> previewInviteCode(@PathVariable String code) {
        return ApiResponse.ok(authService.previewInviteCode(code));
    }
}