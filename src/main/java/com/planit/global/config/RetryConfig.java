package com.planit.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/* DB 마이그레이션 중 쓰기 요청 재시도 설정 
<p>v1 DB를 Read Replica로, v2 DB를 Primary로 승격하는 과정에서
일시적으로 쓰기 요청이 실패할 수 있으므로, 자동 재시도 로직을 제공합니다.

재시도 정책:
재시도 간격: 지수 백오프 (0.3초 → 1.5초 → 7.5초 → 30초 → 30초)
총 대기 시간: 약 69.3초 (1.15분)</li>
재시도 대상 예외: DataAccessException, SQLException 등 DB 관련 예외
 */
@Configuration
@EnableRetry
@ConfigurationProperties(prefix = "planit.db.retry")
@Getter
@Setter
public class RetryConfig {

    /* 재시도 기능 활성화 여부
    마이그레이션 완료 후 false로 변경하여 재시도 로직을 비활성화할 수 있습니다.
     */
    private boolean enabled = true;

    // 최대 재시도 횟수
    private int maxAttempts = 5;

    /* 초기 재시도 간격 (밀리초)
    기본값: 300ms (0.3초)
     */
    private long initialInterval = 300;

    /* 재시도 간격 배수 (지수 백오프)
    기본값: 5.0 (0.3초 → 1.5초 → 7.5초 → 30초 → 30초)
     */
    private double multiplier = 5.0;

    /* 최대 재시도 간격 (밀리초)
    기본값: 30000ms (30초)
     */
    private long maxInterval = 30000;

    /* DB 쓰기 작업용 RetryTemplate 빈
    Repository의 save, delete, saveAndFlush 등의 메서드에서
    DB 연결 실패 시 자동으로 재시도합니다.
    @return 재시도 정책이 설정된 RetryTemplate
    */
    @Bean
    public RetryTemplate dbWriteRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 재시도 정책: 설정값 기반 최대 재시도 횟수 및 재시도 가능한 예외 정의
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts, getRetryableExceptions());
        retryTemplate.setRetryPolicy(retryPolicy);

        // 지수 백오프 정책: 재시도 간격을 점진적으로 증가
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxInterval);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /* 재시도 대상 예외 클래스 맵
    다음 예외 발생 시 재시도:
    DataAccessException: Spring의 DB 접근 예외 (JPA, JDBC 등) - DB 연결 실패, 타임아웃 등
    SQLException: JDBC 레벨 예외 - 네트워크 오류, DB 서버 오류 등
    RuntimeException은 제외 - 비즈니스 예외(IllegalArgumentException 등)는 재시도하지 않음
    @return 예외 클래스와 재시도 여부를 매핑한 맵
    */
    public static Map<Class<? extends Throwable>, Boolean> getRetryableExceptions() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(DataAccessException.class, true);
        retryableExceptions.put(SQLException.class, true);
        return retryableExceptions;
    }
}
