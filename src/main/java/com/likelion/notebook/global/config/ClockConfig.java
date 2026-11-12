package com.likelion.notebook.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * "오늘"은 항상 이 Clock으로 구하기: LocalDate.now(clock)
 * - 기본: 한국 시간 현재
 * - app.today 를 주면 그 날짜로 고정 (시연·로컬에서 데모 데이터 날짜에 맞추기)
 */
@Configuration
public class ClockConfig {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock(@Value("${app.today:}") String today) {
        if (today == null || today.isBlank()) {
            return Clock.system(KST);
        }
        return Clock.fixed(LocalDate.parse(today.trim()).atTime(12, 0).atZone(KST).toInstant(), KST);
    }
}
