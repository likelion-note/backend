package com.likelion.notebook.global;

import com.likelion.notebook.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 배포 후 서버 살아있는지 확인용: GET /api/health */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }
}
