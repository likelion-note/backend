package com.likelion.notebook.user.repository;

import com.likelion.notebook.user.entity.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    /** 지금 배정 (없으면 배정 전) */
    @Query("select a from ProjectAssignment a join fetch a.project where a.user.id = :userId and a.current = true")
    Optional<ProjectAssignment> findCurrent(@Param("userId") Long userId);

    Optional<ProjectAssignment> findByUserIdAndProjectId(Long userId, Long projectId);

    /** 이 사람이 거쳐간 프로젝트 id 목록 (노트 숨김 계산용) */
    @Query("select a.project.id from ProjectAssignment a where a.user.id = :userId")
    List<Long> findProjectIdsByUserId(@Param("userId") Long userId);

    /** 지금 배정된 프로젝트 id 목록 (현재 팀원만 숨길 때) */
    @Query("select a.project.id from ProjectAssignment a where a.user.id = :userId and a.current = true")
    List<Long> findCurrentProjectIdsByUserId(@Param("userId") Long userId);

    /** 프로젝트의 지금 멤버 (운영진 멤버 화면) */
    @Query("select a from ProjectAssignment a join fetch a.user where a.project.id = :projectId and a.current = true")
    List<ProjectAssignment> findCurrentMembers(@Param("projectId") Long projectId);
}
