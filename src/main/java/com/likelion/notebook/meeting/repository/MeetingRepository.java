package com.likelion.notebook.meeting.repository;

import com.likelion.notebook.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByOwnerIdOrderByMeetingDateDesc(Long ownerId);

    /** 성장일지 "회의 불러오기": 지난 7일 (from = 오늘 - 6일) */
    List<Meeting> findByOwnerIdAndMeetingDateBetweenOrderByMeetingDateAsc(Long ownerId, LocalDate from, LocalDate to);
}
