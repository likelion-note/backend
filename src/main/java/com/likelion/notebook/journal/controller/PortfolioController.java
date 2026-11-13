package com.likelion.notebook.journal.controller;

import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.journal.dto.PortfolioProjectsResponse;
import com.likelion.notebook.journal.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
