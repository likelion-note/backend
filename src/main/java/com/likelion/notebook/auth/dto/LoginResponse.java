package com.likelion.notebook.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.notebook.user.dto.MeResponse;

/**
 * status
 * - REGISTERED : 가입된 계정. accessToken + user
 * - NEED_SIGNUP: 처음 로그인. signupToken + email → 초대코드 화면(SCR-ONB-02)으로
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String status, String accessToken, String signupToken, String email, MeResponse user) {

    public static LoginResponse registered(String accessToken, MeResponse user) {
        return new LoginResponse("REGISTERED", accessToken, null, null, user);
    }

    public static LoginResponse needSignup(String signupToken, String email) {
        return new LoginResponse("NEED_SIGNUP", null, signupToken, email, null);
    }
}