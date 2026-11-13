package com.likelion.notebook.journal.controller;

import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.journal.dto.JournalFormResponse;
import com.likelion.notebook.journal.dto.JournalSaveRequest;
import com.likelion.notebook.journal.dto.JournalSaveResponse;
import com.likelion.notebook.journal.dto.JournalSubmitRequest;
import com.likelion.notebook.journal.dto.JournalSummaryResponse;
import com.likelion.notebook.journal.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    /** 1. 작성 화면 기본값 */
    @GetMapping("/form")
    public ApiResponse<JournalFormResponse> form(@CurrentUser Long userId) {
        return ApiResponse.ok(journalService.getForm(userId));
    }

    /** 2. 일지 새로 만들기 (임시 저장) */
    @PostMapping
    public ApiResponse<JournalSaveResponse> create(@CurrentUser Long userId,
                                                   @Valid @RequestBody JournalSaveRequest request) {
        return ApiResponse.ok(journalService.create(userId, request));
    }

    /** 3. 일지 저장 (자동 저장) */
    @PutMapping("/{id}")
    public ApiResponse<JournalSaveResponse> update(@CurrentUser Long userId,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody JournalSaveRequest request) {
        return ApiResponse.ok(journalService.update(userId, id, request));
    }

    /** 4. AI 정리 (Body 없음) */
    @PostMapping("/{id}/summary")
    public ApiResponse<JournalSummaryResponse> summarize(@CurrentUser Long userId, @PathVariable Long id) {
        return ApiResponse.ok(journalService.summarize(userId, id));
    }

    /** 5. 검토 요청 */
    @PostMapping("/{id}/submit")
    public ApiResponse<JournalSaveResponse> submit(@CurrentUser Long userId,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody JournalSubmitRequest request) {
        return ApiResponse.ok(journalService.submit(userId, id, request));
    }
}
