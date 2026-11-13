package com.likelion.notebook.guide.controller;

import com.likelion.notebook.global.auth.AdminChecker;
import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.guide.dto.admin.EventCreateRequest;
import com.likelion.notebook.guide.dto.admin.EventResponse;
import com.likelion.notebook.guide.service.EventService;
import com.likelion.notebook.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final AdminChecker adminChecker;
    private final EventService eventService;

    @GetMapping
    public ApiResponse<List<EventResponse>> list(@CurrentUser Long userId) {
        User admin = adminChecker.require(userId);
        return ApiResponse.ok(eventService.list(admin.getCohort()));
    }

    @PostMapping
    public ApiResponse<EventResponse> create(@CurrentUser Long userId, @Valid @RequestBody EventCreateRequest request) {
        User admin = adminChecker.require(userId);
        return ApiResponse.ok(eventService.create(admin.getCohort(), request));
    }
}
