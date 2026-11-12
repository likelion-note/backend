package com.likelion.notebook.guide.controller;

import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.guide.dto.CheckRequest;
import com.likelion.notebook.guide.dto.GuideResponse;
import com.likelion.notebook.guide.dto.HomeResponse;
import com.likelion.notebook.guide.dto.TaskResponse;
import com.likelion.notebook.guide.service.GuideService;
import com.likelion.notebook.user.entity.Part;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    @GetMapping("/api/home")
    public ApiResponse<HomeResponse> home(@CurrentUser Long userId) {
        return ApiResponse.ok(guideService.home(userId));
    }

    @GetMapping("/api/guide")
    public ApiResponse<GuideResponse> guide(@CurrentUser Long userId,
                                            @RequestParam(required = false) Part part,
                                            @RequestParam(required = false) String stage) {
        return ApiResponse.ok(guideService.guide(userId, part, stage));
    }

    @PutMapping("/api/guide/checks/{itemKey}")
    public ApiResponse<TaskResponse> check(@CurrentUser Long userId,
                                           @PathVariable String itemKey,
                                           @Valid @RequestBody CheckRequest request) {
        return ApiResponse.ok(guideService.check(userId, itemKey, request));
    }
}
