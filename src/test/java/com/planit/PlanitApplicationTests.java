package com.planit; // 테스트 최상위 패키지

import org.junit.jupiter.api.Test; // JUnit 5 테스트 어노테이션
import org.springframework.boot.test.context.SpringBootTest; // 스프링 부트 컨텍스트 테스트

@SpringBootTest // 애플리케이션 컨텍스트 로딩 테스트
class PlanitApplicationTests {

	@Test // 기본 컨텍스트 로딩 검증
	void contextLoads() {
		// 빈 테스트: 빈 성공 여부만 확인
	}

}
