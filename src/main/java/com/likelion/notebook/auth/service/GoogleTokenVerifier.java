package com.likelion.notebook.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.likelion.notebook.global.auth.JwtProvider.GoogleAccount;
import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 프론트가 구글 로그인 버튼으로 받은 id_token을 검증.
 * dev-mode(local)에서는 "dev:<sub>:<email>" 형태의 가짜 토큰도 받음 → 구글 없이 Postman으로 가입 흐름 테스트 가능.
 */
@Component
public class GoogleTokenVerifier {

    private static final String DEV_PREFIX = "dev:";

    private final GoogleIdTokenVerifier verifier;
    private final boolean devMode;

    public GoogleTokenVerifier(@Value("${app.google.client-id}") String clientId,
                               @Value("${app.auth.dev-mode}") boolean devMode) {
        this.devMode = devMode;
        this.verifier = clientId == null || clientId.isBlank()
                ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    public GoogleAccount verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (devMode && idToken.startsWith(DEV_PREFIX)) {
            String[] parts = idToken.split(":", 3); // dev:sub:email
            if (parts.length == 3 && !parts[1].isBlank()) {
                return new GoogleAccount(parts[1], parts[2]);
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (verifier == null) {
            // GOOGLE_CLIENT_ID 환경변수가 비어 있음
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            GoogleIdToken.Payload payload = token.getPayload();
            return new GoogleAccount(payload.getSubject(), payload.getEmail());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}