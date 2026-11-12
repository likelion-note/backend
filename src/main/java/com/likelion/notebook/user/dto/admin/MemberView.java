package com.likelion.notebook.user.dto.admin;

import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;

import java.time.LocalDateTime;

/** 운영진 멤버 화면 한 줄. 이름이 보이는 유일한 곳 */
public record MemberView(Long id, String name, String email, UserType type, Part defaultPart, LocalDateTime joinedAt) {

    public static MemberView from(User u) {
        return new MemberView(u.getId(), u.getName(), u.getEmail(), u.getType(), u.getPart(), u.getCreatedAt());
    }
}
