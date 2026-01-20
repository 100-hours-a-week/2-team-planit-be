# AGENTS.md

## Log
- 2025-02-14: Initialized AGENTS.md per user request (no prior history found).
- 2025-02-14: `./gradlew clean build` 성공했으며 `com.planit` 하위 `UserController`/`UserService` 오류를 확인할 수 없어 증상 재현 정보 요청 중.
- 2026-01-20: `SignUpRequest` DTO를 신규 정의하고 back-end 유효성(아이디/비밀번호/닉네임/프로필 이미지/비밀번호 확인)과 메시지를 기능 정의서에 맞게 맞췄으며 `./gradlew compileJava`를 실행하여 컴파일 성공 확인.
- 2026-01-21: 로그인 ID 허용 범위를 영문 소문자, 숫자, `_`로 좁히고(`SignUpRequest`와 `UserController`), 회원가입/중복확인 전용 `UserService`와 `/users/{signup,check-login-id,check-nickname}` API를 추가하여 중복 메시지·비밀번호 암호화·프로필 이미지 링크 저장을 반영하고 `SecurityConfig`에 `PasswordEncoder` 빈을 등록했으며 `./gradlew compileJava` 확인.
- 2026-01-21: 서버 유효성 정책을 요구사항 기반으로 다시 정리하여 `SignUpRequest`와 `/users/check-login-id` 검증에서 대소문자 모두 허용하도록 수정하고, `UserController`/`UserService`를 재구성하여 회원가입+중복 확인 흐름을 처리한 뒤 `./gradlew compileJava` 재실행으로 통합 성공을 확인.
