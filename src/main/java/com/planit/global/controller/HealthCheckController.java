package com.planit.global.controller; // 공통 컨트롤러 패키지

import java.util.Map; // 응답 body로 Map 사용
import org.springframework.web.bind.annotation.GetMapping; // GET 엔드포인트 매핑
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 선언

@RestController // REST 엔드포인트를 처리하는 컨트롤러
class HealthCheckController {

    @GetMapping({"/health", "/healthcheck"}) // 두 경로에 모두 대응
    public Map<String, String> healthCheck() {
        return Map.of("status", "UP"); // 상태 맵을 반환하여 헬스 체크 통과 알림
    }
}
