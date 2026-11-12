package com.likelion.notebook.meeting.repository;

import com.likelion.notebook.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    /** 지난 회의 목록 (SCR-MEET-05). 최신순 */
    List<Meeting> findByOwnerIdOrderByMeetingDateDescIdDesc(Long ownerId);

    /** 최근 N일 (성장일지 "회의 불러오기"는 7일). 최신순 */
    List<Meeting> findByOwnerIdAndMeetingDateBetweenOrderByMeetingDateDescIdDesc(Long ownerId, LocalDate from, LocalDate to);
}
