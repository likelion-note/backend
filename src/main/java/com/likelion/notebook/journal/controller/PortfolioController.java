package com.likelion.notebook.journal.controller;

import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.journal.dto.PortfolioProjectsResponse;
import com.likelion.notebook.journal.dto.PortfolioSummaryRequest;
import com.likelion.notebook.journal.dto.PortfolioSummaryResponse;
import com.likelion.notebook.journal.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    /** 15. 포트폴리오 프로젝트 목록 */
    @GetMapping("/projects")
    public ApiResponse<PortfolioProjectsResponse> projects(@CurrentUser Long userId) {
        return ApiResponse.ok(portfolioService.projects(userId));
    }

    /** 16. 포트폴리오 요약 만들기 body {projectId, format} */
    @PostMapping("/summaries")
    public ApiResponse<PortfolioSummaryResponse> create(@CurrentUser Long userId,
                                                        @RequestBody PortfolioSummaryRequest request) {
        return ApiResponse.ok(portfolioService.create(userId, request));
    }
}
