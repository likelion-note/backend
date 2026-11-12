package com.likelion.notebook.auth.service;

import com.likelion.notebook.auth.dto.GoogleLoginRequest;
import com.likelion.notebook.auth.dto.InviteCodePreviewResponse;
import com.likelion.notebook.auth.dto.LoginResponse;
import com.likelion.notebook.auth.dto.SignupRequest;
import com.likelion.notebook.auth.entity.InviteCode;
import com.likelion.notebook.auth.repository.InviteCodeRepository;
import com.likelion.notebook.global.auth.JwtProvider;
import com.likelion.notebook.global.auth.JwtProvider.GoogleAccount;
import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.user.dto.MeResponse;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.UserRepository;
import com.likelion.notebook.user.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final AssignmentService assignmentService;
    private final Clock clock;

    /** FN-AUTH-01: 가입한 계정이면 바로 로그인, 처음이면 초대코드 단계로 */
    @Transactional(readOnly = true)
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleAccount account = googleTokenVerifier.verify(request.idToken());
        Optional<User> user = userRepository.findByGoogleSub(account.sub());
        if (user.isPresent()) {
            return loginOf(user.get());
        }
        return LoginResponse.needSignup(jwtProvider.createSignupToken(account.sub(), account.email()), account.email());
    }

    /** FN-AUTH-02, 03: 초대코드 확인 + 이름 입력 → 계정 생성. 초대코드까지 통과해야 users 행이 생김 */
    @Transactional
    public LoginResponse signup(SignupRequest request) {
        GoogleAccount account = jwtProvider.parseSignupToken(request.signupToken());

        // 같은 구글 계정으로 두 번 눌렀을 때는 에러 대신 그냥 로그인
        Optional<User> existing = userRepository.findByGoogleSub(account.sub());
        if (existing.isPresent()) {
            return loginOf(existing.get());
        }

        InviteCode inviteCode = findUsableCode(request.inviteCode());
        User user = userRepository.save(User.builder()
                .email(account.email())
                .googleSub(account.sub())
                .name(request.name().strip())
                .type(inviteCode.getUserType())
                .cohort(inviteCode.getCohort())
                .build());
        inviteCode.increaseUsed();
        return loginOf(user);
    }

    @Transactional(readOnly = true)
    public InviteCodePreviewResponse previewInviteCode(String code) {
        InviteCode inviteCode = findUsableCode(code);
        return new InviteCodePreviewResponse(inviteCode.getCohort(), inviteCode.getUserType());
    }

    private InviteCode findUsableCode(String code) {
        InviteCode inviteCode = inviteCodeRepository.findByCode(code.strip().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "코드를 다시 확인해 주세요"));
        if (!inviteCode.isUsable(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사용이 끝난 코드예요");
        }
        return inviteCode;
    }

    private LoginResponse loginOf(User user) {
        return LoginResponse.registered(jwtProvider.createAccessToken(user.getId()),
                MeResponse.of(user, assignmentService.findCurrent(user.getId()).orElse(null)));
    }
}
