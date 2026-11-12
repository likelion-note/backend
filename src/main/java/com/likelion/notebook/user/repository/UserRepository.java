package com.likelion.notebook.user.repository;

import com.likelion.notebook.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleSub(String googleSub);

    /** 배정 전 멤버. 방금 가입한 사람이 맨 위 */
    List<User> findByCohortAndCurrentProjectIsNullOrderByCreatedAtDesc(Integer cohort);
}
