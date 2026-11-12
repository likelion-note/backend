package com.likelion.notebook.global.auth;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영진 API 맨 앞에서 부르기: User admin = adminChecker.require(userId);
 * 운영진이 아니면 FORBIDDEN. 반환된 User의 기수(cohort)를 기준으로 작업
 */
@Component
@RequiredArgsConstructor
public class AdminChecker {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User require(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "운영진만 할 수 있어요");
        }
        return user;
    }
}
