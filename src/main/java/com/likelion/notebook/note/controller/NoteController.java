package com.likelion.notebook.note.controller;

import com.likelion.notebook.global.auth.CurrentUser;
import com.likelion.notebook.global.common.ApiResponse;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.note.dto.NoteFilter;
import com.likelion.notebook.note.dto.NoteFilter.CohortFilter;
import com.likelion.notebook.note.dto.NoteFilter.NoteSort;
import com.likelion.notebook.note.dto.NoteDetailResponse;
import com.likelion.notebook.note.dto.NoteHelpResponse;
import com.likelion.notebook.note.dto.NoteListResponse;
import com.likelion.notebook.note.service.NoteService;
import com.likelion.notebook.user.entity.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /** 8. 노트 목록·검색 */
    @GetMapping
    public ApiResponse<NoteListResponse> list(@CurrentUser Long userId,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(defaultValue = "ALL") CohortFilter cohort,
                                              @RequestParam(required = false) Part part,
                                              @RequestParam(required = false) Timing timing,
                                              @RequestParam(required = false) Tag tag,
                                              @RequestParam(defaultValue = "NEW") NoteSort sort,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "" + NoteFilter.DEFAULT_SIZE) int size) {
        return ApiResponse.ok(noteService.list(userId,
                new NoteFilter(q, cohort, part, timing, tag, sort, page, size)));
    }

    /** 9. 노트 상세 (열면 읽음 기록) */
    @GetMapping("/{id}")
    public ApiResponse<NoteDetailResponse> detail(@CurrentUser Long userId, @PathVariable Long id) {
        return ApiResponse.ok(noteService.detail(userId, id));
    }

    /** 10. 도움돼요 (누를 때마다 켜짐/꺼짐, Body 없음) */
    @PostMapping("/{id}/help")
    public ApiResponse<NoteHelpResponse> help(@CurrentUser Long userId, @PathVariable Long id) {
        return ApiResponse.ok(noteService.toggleHelp(userId, id));
    }
}
