package com.likelion.notebook.user.repository;

import com.likelion.notebook.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleSub(String googleSub);

    /** 배정 전 아기사자 (지금 배정이 없는 사람). 방금 가입한 사람이 맨 위 */
    @Query("""
            select u from User u
            where u.cohort = :cohort
              and u.type = com.likelion.notebook.user.entity.UserType.MEMBER
              and not exists (select a from ProjectAssignment a where a.user = u and a.current = true)
            order by u.createdAt desc
            """)
    List<User> findUnassigned(@Param("cohort") Integer cohort);

    /** 운영진 수 (2명 이상이면 본인 일지 검토 금지) */
    long countByCohortAndType(Integer cohort, com.likelion.notebook.user.entity.UserType type);
}
