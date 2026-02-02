package com.planit; // 애플리케이션 루트 패키지

import org.springframework.boot.SpringApplication; // SpringApplication 실행 유틸
import org.springframework.boot.autoconfigure.SpringBootApplication; // 스프링 부트 자동 설정
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration; // 기본 보안 설정 제외
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration; // 기본 UserDetailsService 제외
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.planit.domain.user.config.JwtProperties;
import com.planit.domain.user.config.ProfileImageProperties;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class
}) // Spring Boot 자동 설정 활성화 + 기본 Security 제거
@EnableConfigurationProperties({
    ProfileImageProperties.class,
    JwtProperties.class
})
public class PlanitApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanitApplication.class, args); // 애플리케이션 시작
	}

}
