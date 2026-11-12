# 코드 지도 (A 담당 + 공통)

새 파일을 만들거나 로직이 바뀌면 이 문서도 같이 고치기. 경로는 `src/main/java/com/likelion/notebook/` 기준.

마지막 갱신: 로그인 · 에러 코드 5개 · 배정 파트 분리

---

## 전체 흐름 한눈에

```
[프론트] 구글 버튼 → id_token
   → POST /api/auth/google  (GoogleTokenVerifier로 검증 → google_sub로 회원 조회)
        ├ 회원 있음 → REGISTERED + accessToken(7일)
        └ 회원 없음 → NEED_SIGNUP + signupToken(10분)
              → POST /api/auth/signup (초대코드 확인 → User 생성) → REGISTERED + accessToken

[로그인 후 모든 API] Authorization: Bearer {accessToken}
   → CurrentUserArgumentResolver가 검증 → @CurrentUser Long userId 에 주입
   → 실패하면 UNAUTHORIZED(401)
```

---

## global/ (공통)

### global/common/ApiResponse
- 모든 응답의 틀. 성공 `{success:true, data}`, 실패 `{success:false, code, message}`
- 컨트롤러는 항상 `ApiResponse.ok(...)`로 감싸서 반환

### global/common/ErrorCode
- 에러 코드는 **5개만**: `INVALID_INPUT`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `INTERNAL_ERROR`(500)
- 새 코드 추가 금지. 상황별 차이는 message로 구분

### global/exception/BusinessException
- 서비스에서 던지는 유일한 예외
- `new BusinessException(ErrorCode.NOT_FOUND)` → 기본 문구
- `new BusinessException(ErrorCode.INVALID_INPUT, "사유를 한 줄 적어 주세요")` → 지정 문구가 그대로 화면에 나감

### global/exception/GlobalExceptionHandler
- 컨트롤러 밖으로 나온 예외를 잡아서 `ApiResponse.fail` JSON으로 바꿈
- BusinessException → 그 코드와 메시지 / `@Valid` 실패 → INVALID_INPUT / 그 외 → INTERNAL_ERROR(로그 남김)

### global/auth/JwtProvider
- 우리 서버 토큰 2종을 만들고 검증
  - access: subject = userId, `typ=access`, 7일 (`app.jwt.access-token-hours`)
  - signup: subject = googleSub, email 포함, `typ=signup`, 10분
- 검증 시 서명(`JWT_SECRET`), 만료, `typ` 세 가지 확인. 하나라도 틀리면 UNAUTHORIZED
- 서버는 토큰을 저장하지 않음 → 발급된 토큰은 만료 전 취소 불가

### global/auth/CurrentUser
- 컨트롤러 파라미터에 붙이는 표시: `@CurrentUser Long userId`
- **이게 붙은 API만 로그인 필요.** 빠뜨리면 아무나 호출 가능

### global/auth/CurrentUserArgumentResolver
- `@CurrentUser Long` 파라미터에 값을 채움
- 순서: `Authorization: Bearer` → JwtProvider로 검증 → userId / (local만) `X-User-Id` 헤더 / 둘 다 없으면 UNAUTHORIZED
- DB는 안 봄. "누구인지"만 알려주고 "운영진인지"는 서비스에서 따로 확인

### global/config/AuthArgumentConfig
- 위 resolver를 Spring MVC에 등록

---

## auth/ (로그인 · 가입)

### auth/controller/AuthController
- 로그인 전에 부르는 API라 `@CurrentUser` 없음
- `POST /api/auth/google`, `POST /api/auth/signup`, `GET /api/auth/invite-codes/{code}`

### auth/service/GoogleTokenVerifier
- 구글 id_token 검증: 구글 공개키 서명, audience = 우리 `GOOGLE_CLIENT_ID`, 만료
- 통과하면 `GoogleAccount(sub, email)` 반환
- `GOOGLE_CLIENT_ID`가 비어 있으면 INTERNAL_ERROR
- local(dev-mode)에서는 `dev:{sub}:{email}` 가짜 토큰 허용 (구글 없이 테스트)

### auth/service/AuthService
- `googleLogin`: 토큰 검증 → `google_sub`로 회원 조회 → 있으면 accessToken, 없으면 signupToken
- `signup`: signupToken 확인 → 이미 가입된 sub면 그냥 로그인 → 초대코드 확인(없음/꺼짐/만료 → INVALID_INPUT) → User 생성(초대코드의 기수·유형) → 사용 횟수 +1 → accessToken
- `previewInviteCode`: 코드의 기수·유형만 반환 (가입 화면 확인 문구)
- 회원 판별은 이메일이 아니라 `google_sub` (이메일은 바뀔 수 있음)
- users 행은 초대코드 통과 후에만 생김

### auth/dto/
| 파일 | 내용 |
|---|---|
| GoogleLoginRequest | `idToken` |
| SignupRequest | `signupToken`, `inviteCode`(6자리), `name`(30자 이하) |
| LoginResponse | `status`(REGISTERED / NEED_SIGNUP) + 상황에 맞는 필드만 (null 필드는 응답에서 빠짐) |
| InviteCodePreviewResponse | `cohort`, `userType` |

### auth/LocalInviteCodeSeeder
- local 프로필에서만 실행. 초대코드가 비어 있으면 `seed/demo-data.json`의 코드(LION14, STAF14, LION13) 등록

---

## user/ (사용자 · 배정)

### user/entity/User
- `part` = **기본 파트** (운영진이 지정, 배정 전엔 null). 운영진은 가입 시 OPS
- 화면의 "지금 파트"는 이 값이 아니라 지금 배정의 `ProjectAssignment.part`
- `changeDefaultPart()`: 기본 파트만 바꿈. 이미 배정된 파트는 그대로
- 이름은 본인 정보·운영진 멤버 목록에서만. 노트·검토 응답에 넣지 않기

### user/entity/ProjectAssignment
- 배정 = 사람 × 프로젝트 분류 × 그 프로젝트에서의 파트
- `is_current = true`인 행이 지금 배정 (사람당 최대 1개)
- 재배정해도 이전 행은 지우지 않고 `is_current = false` (이력. 이전 일지·노트 숨김에 사용)
- UNIQUE(user_id, project_id): 예전 프로젝트로 돌아가면 새 행 대신 그 행을 다시 켬(`reactivate`)

### user/service/AssignmentService
- `assign(userId, projectId, part)`
  - 운영(OPS) 분류 → 파트는 무조건 OPS
  - part가 null이면 기본 파트 복사. 기본 파트도 없으면 INVALID_INPUT "파트를 골라 주세요"
  - 같은 프로젝트 재배정 → 파트만 변경 / 다른 프로젝트 → 지금 배정 종료 후 새 배정(또는 예전 행 재사용)
- `unassign(userId)`: 지금 배정 종료 → 배정 전 상태
- `changeDefaultPart(userId, part)`
- `findCurrent(userId)`: 지금 배정 (없으면 Optional.empty)

### user/repository/ProjectAssignmentRepository
| 메서드 | 용도 |
|---|---|
| findCurrent | 지금 배정 (프로젝트 같이 조회) |
| findByUserIdAndProjectId | 예전 배정 재사용 확인 |
| findProjectIdsByUserId | 거쳐 간 모든 프로젝트 → 지난 팀원까지 노트 숨김 |
| findCurrentProjectIdsByUserId | 지금 프로젝트 → 현재 팀원만 노트 숨김 |
| findCurrentMembers | 프로젝트의 지금 멤버 (운영진 멤버 화면) |

### user/repository/UserRepository
| 메서드 | 용도 |
|---|---|
| findByGoogleSub | 로그인 시 회원 판별 |
| findUnassigned | 지금 배정이 없는 아기사자, 최근 가입순 (운영진 "배정 전" 그룹) |
| countByCohortAndType | 운영진 수 → 2명 이상이면 본인 일지 검토 금지 |

### user/controller/MeController
- `GET /api/me`: 내 정보 + 기본 파트 + 지금 배정. 배정 전이면 `part`, `project`가 null

### user/dto/MeResponse
- `id, name, type, cohort, defaultPart, part, project{id, title}`
- 본인 전용이라 이름 포함

---

## 설정

### application.yml
| 키 | 의미 | 배포 환경변수 |
|---|---|---|
| app.google.client-id | 구글 OAuth 클라이언트 ID | `GOOGLE_CLIENT_ID` |
| app.jwt.secret | 토큰 서명 키 (32자 이상) | `JWT_SECRET` |
| app.jwt.access-token-hours | accessToken 유효시간 | `JWT_ACCESS_HOURS` (기본 168) |
| app.jwt.signup-token-minutes | signupToken 유효시간 | 10 고정 |
| app.auth.dev-mode | `X-User-Id`, `dev:` 토큰 허용 | local 구역에서만 true. prod는 false |

---

## 테스트

| 파일 | 확인하는 것 |
|---|---|
| auth/AuthFlowTest | 가짜 구글 토큰 → NEED_SIGNUP → 틀린 코드 400 → 가입 → JWT로 /api/me → 재로그인 REGISTERED → 토큰 없이 401 |
| user/AssignmentServiceTest | 기본 파트 복사, 재배정 이력, 배정 해제, 파트 없음 에러, 운영 분류는 OPS |
