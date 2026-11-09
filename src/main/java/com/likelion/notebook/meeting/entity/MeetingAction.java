package com.likelion.notebook.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MeetingAction {

    @Column(length = 30)
    private String who;

    @Column(length = 200)
    private String what;

    @Column(length = 10)
    private String due; // "월"~"일"
}
