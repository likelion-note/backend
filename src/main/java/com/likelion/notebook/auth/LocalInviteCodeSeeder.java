package com.likelion.notebook.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.notebook.auth.entity.InviteCode;
import com.likelion.notebook.auth.repository.InviteCodeRepository;
import com.likelion.notebook.user.entity.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;

/** local 프로필에서만: demo-data.json의 초대코드를 DB에 넣어서 바로 가입 테스트 가능하게 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalInviteCodeSeeder implements CommandLineRunner {

    private final InviteCodeRepository inviteCodeRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (inviteCodeRepository.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource("seed/demo-data.json").getInputStream()) {
            JsonNode codes = objectMapper.readTree(in).path("inviteCodes");
            for (JsonNode c : codes) {
                inviteCodeRepository.save(InviteCode.builder()
                        .code(c.get("code").asText())
                        .userType(UserType.valueOf(c.get("userType").asText()))
                        .cohort(c.get("cohort").asInt())
                        .expiresAt(LocalDate.parse(c.get("expiresAt").asText()))
                        .build());
            }
            log.info("[local] 초대코드 {}개 넣음", codes.size());
        }
    }
}