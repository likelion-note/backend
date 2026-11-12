package com.likelion.notebook.meeting.controller;

import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.meeting.dto.MeetingCreateRequest;
import com.likelion.notebook.meeting.dto.MeetingResponse;
import com.likelion.notebook.meeting.dto.MeetingTemplateResponse;
import com.likelion.notebook.meeting.service.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    /** 템플릿은 시드 데이터라 로그인 정보가 필요 없지만, 앱 안 화면이라 로그인 사용자만 */
    @GetMapping("/templates")
    public ApiResponse<MeetingTemplateResponse> templates(@CurrentUser Long userId) {
        return ApiResponse.ok(meetingService.templates());
    }

    @PostMapping
    public ApiResponse<MeetingResponse> create(@CurrentUser Long userId,
                                               @Valid @RequestBody MeetingCreateRequest request) {
        return ApiResponse.ok(meetingService.create(userId, request));
    }

    @GetMapping
    public ApiResponse<List<MeetingResponse>> list(@CurrentUser Long userId,
                                                   @RequestParam(required = false) Integer days) {
        return ApiResponse.ok(meetingService.list(userId, days));
    }

    @GetMapping("/{meetingId}")
    public ApiResponse<MeetingResponse> get(@CurrentUser Long userId, @PathVariable Long meetingId) {
        return ApiResponse.ok(meetingService.get(userId, meetingId));
    }

    @DeleteMapping("/{meetingId}")
    public ApiResponse<Void> delete(@CurrentUser Long userId, @PathVariable Long meetingId) {
        meetingService.delete(userId, meetingId);
        return ApiResponse.ok();
    }
}
