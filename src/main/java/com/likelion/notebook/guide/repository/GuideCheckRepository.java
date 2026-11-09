package com.likelion.notebook.guide.repository;

import com.likelion.notebook.guide.entity.GuideCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GuideCheckRepository extends JpaRepository<GuideCheck, Long> {

    Optional<GuideCheck> findByUserIdAndItemKey(Long userId, String itemKey);

    @Query("select g.itemKey from GuideCheck g where g.user.id = :userId")
    List<String> findCheckedKeys(Long userId);
}
