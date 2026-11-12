package com.likelion.notebook.user.controller;

import com.likelion.notebook.global.auth.AdminChecker;
import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.user.dto.admin.AssignRequest;
import com.likelion.notebook.user.dto.admin.AssignmentResponse;
import com.likelion.notebook.user.dto.admin.DefaultPartRequest;
import com.likelion.notebook.user.dto.admin.MemberView;
import com.likelion.notebook.user.dto.admin.MembersResponse;
import com.likelion.notebook.user.dto.admin.ProjectCreateRequest;
import com.likelion.notebook.user.dto.admin.ProjectResponse;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.service.MemberService;
import com.likelion.notebook.user.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 운영진 전용. 모든 메서드가 adminChecker.require로 시작 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminMemberController {

    private final AdminChecker adminChecker;
    private final ProjectService projectService;
    private final MemberService memberService;

    @GetMapping("/projects")
    public ApiResponse<List<ProjectResponse>> projects(@CurrentUser Long userId) {
        User admin = adminChecker.require(userId);
        return ApiResponse.ok(projectService.list(admin.getCohort()));
    }

    @PostMapping("/projects")
    public ApiResponse<ProjectResponse> createProject(@CurrentUser Long userId,
                                                      @Valid @RequestBody ProjectCreateRequest request) {
        User admin = adminChecker.require(userId);
        return ApiResponse.ok(projectService.create(admin.getCohort(), request));
    }

    @GetMapping("/members")
    public ApiResponse<MembersResponse> members(@CurrentUser Long userId) {
        User admin = adminChecker.require(userId);
        return ApiResponse.ok(memberService.members(admin.getCohort()));
    }

    @PutMapping("/members/{memberId}/assignment")
    public ApiResponse<AssignmentResponse> assign(@CurrentUser Long userId, @PathVariable Long memberId,
                                                  @Valid @RequestBody AssignRequest request) {
        User admin = adminChecker.require(userId);
        return ApiResponse.ok(memberService.assign(admin.getCohort(), memberId, request.projectId(), request.part()));
    }

    @DeleteMapping("/members/{memberId}/assignment")
    public ApiResponse<Void> unassign(@CurrentUser Long userId, @PathVariable Long memberId) {
        User admin = adminChecker.require(userId);
        memberService.unassign(admin.getCohort(), memberId);
        return ApiResponse.ok();
    }

    @PutMapping("/members/{memberId}/default-part")
    public ApiResponse<MemberView> defaultPart(@CurrentUser Long userId, @PathVariable Long memberId,
                                               @Valid @RequestBody DefaultPartRequest request) {
        User admin = adminChecker.require(userId);
        return ApiResponse.ok(memberService.changeDefaultPart(admin.getCohort(), memberId, request.part()));
    }
}
