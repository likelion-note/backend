package com.likelion.notebook.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러에서 로그인한 사용자 id 받기: public ... create(@CurrentUser Long userId, ...)
 *
 * 지금은 로그인 전이라 CurrentUserArgumentResolver가 "X-User-Id" 헤더 값을 그대로 씀 (B가 A 기다리지 않게).
 * A가 JWT 붙이면 resolver 안쪽만 바꾸면 되고, 이 어노테이션을 쓰는 컨트롤러 코드는 그대로.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
