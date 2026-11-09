package com.likelion.notebook.guide.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import com.likelion.notebook.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오늘 할 일 체크 (개인별). 가이드 항목 자체는 MVP에서 시드 데이터(코드/JSON)로 두고,
 * 여기엔 "누가 어떤 항목을 체크했는지"만 저장. itemKey 예: "PM-w2-0"
 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideCheck extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "item_key", nullable = false, length = 40)
    private String itemKey;

    public GuideCheck(User user, String itemKey) {
        this.user = user;
        this.itemKey = itemKey;
    }
}
