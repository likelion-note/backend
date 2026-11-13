package com.likelion.notebook.user.repository;

import com.likelion.notebook.user.entity.ProjectCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCategoryRepository extends JpaRepository<ProjectCategory, Long> {

    List<ProjectCategory> findByCohortOrderByIdAsc(Integer cohort);

    /** 팀 글자(A, B, C…) 자동 부여용 */
    long countByCohortAndKind(Integer cohort, ProjectCategory.ProjectKind kind);
}
