package com.likelion.notebook.user.repository;

import com.likelion.notebook.user.entity.ProjectCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCategoryRepository extends JpaRepository<ProjectCategory, Long> {

    List<ProjectCategory> findByCohortOrderByIdAsc(Integer cohort);
}
