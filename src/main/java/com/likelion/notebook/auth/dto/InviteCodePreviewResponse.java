package com.likelion.notebook.auth.dto;

import com.likelion.notebook.user.entity.UserType;

/** 초대코드 화면의 "14기 아기사자로 가입합니다" 확인 문구용 */
public record InviteCodePreviewResponse(Integer cohort, UserType userType) {
}