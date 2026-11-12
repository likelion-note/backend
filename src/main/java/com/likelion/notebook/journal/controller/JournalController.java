package com.likelion.notebook.journal.controller;

import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.journal.dto.JournalFormResponse;
import com.likelion.notebook.journal.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
