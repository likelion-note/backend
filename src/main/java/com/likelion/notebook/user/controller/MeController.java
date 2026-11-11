package com.likelion.notebook.user.controller;

import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.user.dto.MeResponse;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.UserRepository;
import com.likelion.notebook.user.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;

    /** 앱 진입 시 내 정보 + 지금 배정. 배정 전이면 part·project가 null */
    @GetMapping("/api/me")
    @Transactional(readOnly = true)
    public ApiResponse<MeResponse> me(@CurrentUser Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return ApiResponse.ok(MeResponse.of(user, assignmentService.findCurrent(userId).orElse(null)));
    }
}
