package com.likelion.notebook.user.repository;

import com.likelion.notebook.user.entity.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    boolean existsByUserIdAndProjectId(Long userId, Long projectId);

    /** 이 사람이 거쳐간 프로젝트 id 목록 (노트 숨김 계산용) */
    @Query("select a.project.id from ProjectAssignment a where a.user.id = :userId")
    List<Long> findProjectIdsByUserId(Long userId);
}
