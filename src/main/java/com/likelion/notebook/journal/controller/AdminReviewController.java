package com.likelion.notebook.journal.controller;

import com.likelion.notebook.global.auth.AdminChecker;
import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.journal.dto.ReviewDetailResponse;
import com.likelion.notebook.journal.dto.ReviewListResponse;
import com.likelion.notebook.journal.dto.ReviewRejectRequest;
import com.likelion.notebook.journal.dto.ReviewResultResponse;
import com.likelion.notebook.journal.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영진 전용. 모든 메서드가 adminChecker.require로 시작 (운영진 아니면 FORBIDDEN) */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminChecker adminChecker;
    private final ReviewService reviewService;

    /** 11. 검토 목록 */
    @GetMapping
    public ApiResponse<ReviewListResponse> list(@CurrentUser Long userId) {
        return ApiResponse.ok(reviewService.list(adminChecker.require(userId)));
    }

    /** 12. 검토 상세 */
    @GetMapping("/{journalId}")
    public ApiResponse<ReviewDetailResponse> detail(@CurrentUser Long userId, @PathVariable Long journalId) {
        return ApiResponse.ok(reviewService.detail(adminChecker.require(userId), journalId));
    }

    /** 13. 승인 (Body 없음) */
    @PostMapping("/{journalId}/approve")
    public ApiResponse<ReviewResultResponse> approve(@CurrentUser Long userId, @PathVariable Long journalId) {
        return ApiResponse.ok(reviewService.approve(adminChecker.require(userId), journalId));
    }

    /** 14. 돌려보내기 body {reason} */
    @PostMapping("/{journalId}/reject")
    public ApiResponse<ReviewResultResponse> reject(@CurrentUser Long userId, @PathVariable Long journalId,
                                                    @RequestBody ReviewRejectRequest request) {
        return ApiResponse.ok(reviewService.reject(adminChecker.require(userId), journalId, request.reason()));
    }
}
