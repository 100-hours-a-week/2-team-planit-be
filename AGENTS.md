## 공통 Error Code 규칙

### 설계 원칙

- **도메인별 Prefix**
- **숫자는 의미 단위**
- 프론트에서 **분기 처리 가능**해야 함

---

### ErrorCode 네이밍 규칙

```
<DOMAIN>_<NUMBER>
```

### Domain Prefix

| 도메인 | Prefix |
| --- | --- |
| 인증 | AUTH |
| 사용자 | USER |
| 게시판 | POST |
| 알림 | NOTI |
| 공통 | COMMON |

---

### 예시 ErrorCode Enum

```java
publicenumErrorCode {

// COMMON
    INVALID_REQUEST(COMMON_001,"잘못된 요청입니다"),
    INTERNAL_ERROR(COMMON_999,"서버 오류가 발생했습니다"),

// AUTH
    INVALID_TOKEN(AUTH_001,"유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(AUTH_002,"토큰이 만료되었습니다"),
    UNAUTHORIZED(AUTH_003,"인증이 필요합니다"),

// USER
    USER_NOT_FOUND(USER_001,"사용자를 찾을 수 없습니다"),
    DUPLICATE_LOGIN_ID(USER_002,"이미 사용 중인 아이디입니다"),

// POST
    POST_NOT_FOUND(POST_001,"게시글을 찾을 수 없습니다"),
    FORBIDDEN_POST_ACCESS(POST_002,"게시글에 대한 권한이 없습니다"),

// NOTIFICATION
    NOTIFICATION_NOT_FOUND(NOTI_001,"알림을 찾을 수 없습니다");
}

```

- 메시지는 “사람용”
- code는 “프론트/로직용”

---

## 공통 Response 규칙

### 성공 응답

```json
{
"message":"SUCCESS",
"data":{}
}
```

### 실패 응답

```json
{
"message":"AUTH_001",
"error":{
	"code":"AUTH_001",
	"message":"유효하지 않은 토큰입니다"
	}
}
```

프론트에서

- message → 공통 처리
- error.code → 분기 처리
- error.message → 사용자 노출