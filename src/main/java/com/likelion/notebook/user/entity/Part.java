package com.likelion.notebook.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Part {
    PM("PM"),
    DESIGN("디자인"),
    FRONTEND("프론트"),
    BACKEND("백엔드"),
    OPS("운영진");

    private final String label;
}
