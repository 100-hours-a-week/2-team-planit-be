package com.planit.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * Repository 쓰기 작업에 재시도 로직을 적용하는 AOP Aspect
 * 
 * <p>DB 마이그레이션 중 일시적인 연결 실패 시 자동으로 재시도합니다.
 * 
 * <p>적용 대상:
 * <ul>
 *   <li>JpaRepository의 save, saveAll, saveAndFlush 메서드</li>
 *   <li>JpaRepository의 delete, deleteAll, deleteById 메서드</li>
 *   <li>@Modifying이 붙은 커스텀 쿼리 메서드 (UPDATE, DELETE 쿼리)</li>
 * </ul>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseWriteRetryAspect {

    private final RetryTemplate dbWriteRetryTemplate;
    private final RetryConfig retryConfig;

    /**
     * Repository의 save 관련 메서드에 재시도 로직 적용
     * 
     * @param joinPoint AOP 조인 포인트
     * @return 메서드 실행 결과
     * @throws Throwable 재시도 실패 시 예외
     */
    @Around("execution(* org.springframework.data.repository.Repository+.save*(..))")
    public Object retrySaveOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // 재시도 기능이 비활성화되어 있으면 바로 실행
        if (!retryConfig.isEnabled()) {
            return joinPoint.proceed();
        }
        return executeWithRetry(joinPoint, "save");
    }

    /**
     * Repository의 delete 관련 메서드에 재시도 로직 적용
     * 
     * @param joinPoint AOP 조인 포인트
     * @return 메서드 실행 결과
     * @throws Throwable 재시도 실패 시 예외
     */
    @Around("execution(* org.springframework.data.repository.Repository+.delete*(..))")
    public Object retryDeleteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // 재시도 기능이 비활성화되어 있으면 바로 실행
        if (!retryConfig.isEnabled()) {
            return joinPoint.proceed();
        }
        return executeWithRetry(joinPoint, "delete");
    }

    /**
     * @Modifying이 붙은 커스텀 쿼리 메서드에 재시도 로직 적용
     * 
     * <p>UPDATE, DELETE 쿼리 등 @Modifying 어노테이션이 붙은 메서드도 재시도 대상에 포함합니다.
     * 예: incrementCommentCount(), updateReadStatus() 등
     * 
     * @param joinPoint AOP 조인 포인트
     * @return 메서드 실행 결과
     * @throws Throwable 재시도 실패 시 예외
     */
    @Around("@annotation(org.springframework.data.jpa.repository.Modifying) && " +
            "execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object retryModifyingOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // 재시도 기능이 비활성화되어 있으면 바로 실행
        if (!retryConfig.isEnabled()) {
            return joinPoint.proceed();
        }
        return executeWithRetry(joinPoint, "modifying");
    }

    /**
     * 재시도 로직을 포함한 메서드 실행
     * 
     * <p>재시도 흐름:
     * <ol>
     *   <li>최대 5회까지 재시도 (설정값 기반)</li>
     *   <li>각 재시도마다 지수 백오프로 대기 (0.3초 → 1.5초 → 7.5초 → 30초 → 30초)</li>
     *   <li>총 대기 시간: 약 69.3초 (1.15분)</li>
     *   <li>모든 재시도 실패 시: 마지막 예외를 그대로 전파하고 에러 로그 기록</li>
     *   <li>예외는 서비스 레이어 → 컨트롤러 → GlobalExceptionHandler로 전파됨</li>
     * </ol>
     * 
     * @param joinPoint AOP 조인 포인트
     * @param operationType 작업 유형 (save, delete 등)
     * @return 메서드 실행 결과
     * @throws Throwable 모든 재시도 실패 시 마지막 예외를 그대로 전파
     */
    private Object executeWithRetry(ProceedingJoinPoint joinPoint, String operationType) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        
        try {
            return dbWriteRetryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                
                if (attempt > 1) {
                    log.warn("[DB Write Retry] {} 작업 재시도 중... (시도 횟수: {}/{})", 
                            methodName, attempt, retryConfig.getMaxAttempts());
                }
                
                try {
                    Object result = joinPoint.proceed();
                    
                    if (attempt > 1) {
                        log.info("[DB Write Retry] {} 작업 재시도 성공 (시도 횟수: {})", 
                                methodName, attempt);
                    }
                    
                    return result;
                } catch (Throwable e) {
                    // 재시도 가능한 예외인지 확인
                    if (isRetryableException(e)) {
                        log.warn("[DB Write Retry] {} 작업 실패 (시도 횟수: {}): {}", 
                                methodName, attempt, e.getMessage());
                        // RetryTemplate이 재시도할 수 있도록 예외를 그대로 전파
                        // (RetryTemplate의 예외 분류기가 처리함)
                        throw e;
                    } else {
                        // 재시도 불가능한 예외는 그대로 전파
                        log.error("[DB Write Retry] {} 작업 실패 (재시도 불가): {}", 
                                methodName, e.getMessage(), e);
                        throw e;
                    }
                }
            });
        } catch (Throwable e) {
            // 모든 재시도가 실패한 경우 (최대 재시도 횟수 초과)
            log.error("[DB Write Retry] {} 작업 최종 실패 - 모든 재시도 시도 실패 (최대 재시도 횟수: {}회). " +
                            "DB 마이그레이션 중일 수 있습니다. 원본 예외를 확인하세요.", 
                    methodName, retryConfig.getMaxAttempts(), e);
            // 원본 예외를 그대로 전파하여 서비스 레이어에서 처리할 수 있도록 함
            throw e;
        }
    }

    /**
     * 예외가 재시도 가능한지 확인
     * 
     * <p>다음 예외만 재시도 대상으로 판단:
     * <ul>
     *   <li>DataAccessException: Spring의 DB 접근 예외 (JPA, JDBC 등)</li>
     *   <li>SQLException: JDBC 레벨 예외</li>
     * </ul>
     * 
     * <p>예외의 원인(cause)도 재귀적으로 확인하여 래핑된 예외도 처리합니다.
     * 
     * @param throwable 발생한 예외
     * @return 재시도 가능 여부
     */
    private boolean isRetryableException(Throwable throwable) {
        // DataAccessException 계열 (Spring의 DB 접근 예외)
        if (throwable instanceof DataAccessException) {
            return true;
        }
        
        // SQLException 계열 (JDBC 레벨 예외)
        if (throwable instanceof SQLException) {
            return true;
        }
        
        // 원인 예외도 확인
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return isRetryableException(cause);
        }
        
        return false;
    }
}
