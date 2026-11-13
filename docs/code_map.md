# 코드 지도 (A 담당 + 공통)

새 파일을 만들거나 로직이 바뀌면 이 문서도 같이 고치기. 경로는 `src/main/java/com/likelion/notebook/` 기준.

마지막 갱신: 로그인 · 에러 코드 5개 · 배정 파트 분리 · 홈/가이드 · 운영진(프로젝트·멤버·일정) · 회의

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

### global/auth/AdminChecker
- 운영진 API 맨 앞에서 `User admin = adminChecker.require(userId)` 호출
- 운영진이 아니면 FORBIDDEN. 반환된 운영진의 기수(cohort) 안에서만 작업 (다른 기수 데이터는 NOT_FOUND)
- B의 검토 API도 이걸 쓰면 됨

### global/config/AuthArgumentConfig
- 위 resolver를 Spring MVC에 등록

### global/config/ClockConfig
- "오늘"은 항상 `LocalDate.now(clock)`으로 구하기 (`LocalDate.now()` 직접 쓰지 않기)
- `app.today`가 있으면 그 날짜로 고정. local은 데모 데이터 기준일 2026-10-27, prod는 비움(실제 오늘). 시연 때 날짜 고정하려면 `APP_TODAY`

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

### user/service/ProjectService (FN-ADM-02)
- `create`: 운영진 본인 기수에 생성. TEAM은 teamLetter를 A, B, C… 자동 부여 (기수 안 TEAM 개수 기준), OPS는 null
- `list`: 기수의 분류 + 지금 멤버 수

### user/service/MemberService (FN-ADM-03)
- `members`: 배정 전(최근 가입순) / 팀별 → 파트별(PM, DESIGN, FRONTEND, BACKEND) / 운영 분류별
- `assign`: 같은 기수만. 팀에 OPS 파트 불가. 나머지 규칙은 AssignmentService
- `changeDefaultPart`: 아기사자에게 OPS 불가
- `unassign`: 배정 해제

### user/controller/AdminMemberController
| API | 화면 |
|---|---|
| `GET /api/admin/projects` | SCR-ADM-04, 05 |
| `POST /api/admin/projects` body `{title, kind}` | SCR-ADM-06 |
| `GET /api/admin/members` | SCR-ADM-04 |
| `PUT /api/admin/members/{id}/assignment` body `{projectId, part?}` | SCR-ADM-05 |
| `DELETE /api/admin/members/{id}/assignment` | 배정 해제 |
| `PUT /api/admin/members/{id}/default-part` body `{part}` | SCR-ADM-05 |

### user/controller/MeController
- `GET /api/me`: 내 정보 + 기본 파트 + 지금 배정. 배정 전이면 `part`, `project`가 null

### user/dto/MeResponse
- `id, name, type, cohort, defaultPart, part, project{id, title}`
- 본인 전용이라 이름 포함

---

## guide/ (홈 · 파트 가이드 · 일정)

### guide/seed/GuideSeed
- `seed/guide.json`을 앱 시작 시 읽어 메모리에 둠 (DB에 안 넣음)
- stages: 주차 구간 6개(W1, W2, W3, W4_5, W6, W7) + 구간별 운영진 한마디(opTip)
- items: 파트 × 구간 할 일 69개. `key`(예: `PM-W2-0`)가 GuideCheck.itemKey. 순서 바꾸면 기존 체크가 엉키니 추가는 맨 뒤에
- docs: 지난 기수 자료 6개. 할 일의 `action.type = DOC`이 `docKey`로 가리킴
- action.type = MEETING이면 `meetingType`으로 회의 도우미 이동

### guide/service/StageService (FN-MATCH-01)
- 기수의 일정(events)과 오늘 날짜로 계산. 저장하지 않음
- 주차: PROJECT_START 일정이 1주차. 시작 전이면 null
- 구간: 1→W1, 2→W2, 3→W3, 4·5→W4_5, 6→W6, 7 이상→W7
- D-day: 오늘 이후 가장 가까운 큰 행사(PROJECT_START, SESSION 제외)
- 다음 정기 세션: D-day와 따로
- 시점(Timing): 다음 큰 행사까지 14일 이상 W2PLUS, 7~13일 W1_2, 0~6일 DURING, 남은 행사 없으면 AFTER
- 프로젝트 기간: 시작일이 지났고 남은 큰 행사가 있을 때. 아니면 홈에 "지금은 프로젝트 기간이 아니에요"
- **B 일지 작성의 시점 기본값도 이걸 쓰기**: `stageService.current(cohort).timing()`

### guide/service/GuideService
- `home`: 운영진이면 FORBIDDEN. 배정 전이거나 기간 밖이면 할 일 빈 배열. 할 일 = 지금 배정 파트 × 이번 구간
- `guide`: part·stage를 안 주면 내 파트·이번 구간. 할 일이 가리키는 자료(docs)만 같이 내려줌
- `check`: true면 체크 저장, false면 삭제. 같은 요청을 여러 번 보내도 결과 같음. 없는 키면 NOT_FOUND

### guide/controller/GuideController
| API | 화면 |
|---|---|
| `GET /api/home` | SCR-HOME-01 (A 부분: 배정, 주차·D-day, 할 일, 운영진 한마디). B가 배너·노트 반응·추천 노트 필드 추가 예정 |
| `GET /api/guide?part=&stage=` | SCR-GUIDE-01, 02 |
| `PUT /api/guide/checks/{itemKey}` body `{checked}` | 홈·가이드 체크박스 |

### guide/service/EventService (FN-ADM-04)
- `list`: 기수 일정 날짜순
- `create`: PROJECT_START는 기수마다 하나 (중복이면 INVALID_INPUT). 저장 즉시 모든 홈에 반영(홈이 매번 계산)

### guide/controller/AdminEventController
| API | 화면 |
|---|---|
| `GET /api/admin/events` | SCR-ADM-07 |
| `POST /api/admin/events` body `{name, type, eventDate}` | SCR-ADM-07 |

### guide/LocalEventSeeder
- local에서만 demo-data.json 일정 5개 등록 (시작 10/19, 세션 10/29, 중간발표 11/08, 해커톤 11/21, 데모데이 12/05)

---

## meeting/ (회의 도우미 · 회의 정리)

### meeting/seed/MeetingSeed
- `seed/meeting-helper.json`을 시작 시 메모리로 로드 (DB에 안 넣음)
- 회의 종류 4개(FIRST, IDEA, CHECK, RETRO)마다 오늘 정할 질문 3개 + 막힐 때 팁
- 팁의 `tags`(상황 태그 enum)로 관련 노트를 찾음 → 프론트가 노트 게시판 API를 tag로 조회 (B 담당)

### meeting/service/MeetingService
- `create`: 빈 줄은 버림. 정한 것과 할 일이 둘 다 비면 INVALID_INPUT. 날짜 생략 시 오늘
- `list(userId, days)`: days 없으면 전체, 있으면 오늘 포함 최근 N일. 최신순
- `get`, `delete`: 본인 것만. 남의 회의는 NOT_FOUND (존재 자체를 숨김)
- **B의 성장일지 "회의 불러오기"는 `meetingService.list(userId, 7)`을 쓰기**

### meeting/dto/MeetingResponse
- `shareText`: 공유하기 버튼이 그대로 복사할 원문 (FN-MEET-04). 서버가 고정 형식으로 만듦
  ```
  아이디어 회의 · 10/27

  정한 것
  - 주제는 동아리 일정 관리로

  누가 언제까지
  - 민수 · 경쟁 서비스 조사 (수)
  ```
- 할 일의 `due`는 요일 한 글자("월"~"일") 같은 짧은 문자열 (날짜 아님)

### meeting/dto/MeetingLabels
- 회의 종류 한글 이름 (첫 회의, 아이디어 회의, 중간 점검 회의, 회고 회의)

### meeting/controller/MeetingController
| API | 화면 |
|---|---|
| `GET /api/meetings/templates` | SCR-MEET-01, 02 |
| `POST /api/meetings` | SCR-MEET-03 → 응답의 shareText로 SCR-MEET-04 |
| `GET /api/meetings?days=7` | SCR-MEET-05 (days 없이), SCR-JOUR-01 회의 불러오기 (days=7) |
| `GET /api/meetings/{id}` | SCR-MEET-05 펼치기 |
| `DELETE /api/meetings/{id}` | 잘못 저장한 회의 삭제 |

---

## journal/ (B)

### journal/service/JournalService
- `getForm` (1번 API): 작성 화면 기본값
  - projects = 배정된 적 있는 프로젝트 전부(지난 배정·운영 분류 포함). 지금 배정이 맨 앞, 나머지는 최근 배정 순
  - defaultProjectId·defaultPart = 지금 배정. 없으면 null / 기본 파트(users.part)
  - defaultTiming = `stageService.current(cohort).timing()`. 그 기수 일정이 아예 없으면 null
  - A의 `ProjectAssignmentRepository`는 수정하지 않고 기존 메서드(`findProjectIdsByUserId` + `findByUserIdAndProjectId`) 조합
- `create` (2번 API): DRAFT 일지 생성. projectId는 배정된 적 있는 프로젝트만(아니면 INVALID_INPUT), null = 미분류
- `update` (3번 API): 보낸 body 전체로 덮어씀(안 보낸 칸은 null). DRAFT·REJECTED만. AI 정리본은 그대로
- `summarize` (4번 API): `Journal.checkReadyForSummary`(상태 → 활동·시점·공개 범위 → 막힌 것 30자 → 넘긴 방법) → `AiClient` → `applySummary`. 보완 질문은 처음 정리할 때만(저장 안 함). `AiClientException`이면 INTERNAL_ERROR, 원문 유지. 태그는 중복 빼고 최대 3개
- `submit` (5번 API): `Journal.submit(keptTags)` → 태그는 빼기만(새 태그면 400), PENDING으로, 돌려받은 사유 지움
- `getMine` (6번 API): 내 일지 전부(프로젝트 fetch join, `updatedAt` 최신순) + 상태별 개수 + `noteImpact`
- `getDetail` (7번 API): 원문 + 정리본(정리 전이면 summary null) + projectId. 남의 일지 403 "내 일지만 볼 수 있어요"
- **파트 A 홈에서 부를 메서드**
  - `journalBanner(userId)` → `{rejected, pending}` 내 일지 개수
  - `noteImpact(userId)` → `{readCount, helpCount}` 내 공개(APPROVED) 노트의 `note_reaction` 합계 (행 = 읽음, `helped` = 도움돼요)
- `findMine`: 없으면 NOT_FOUND "일지를 찾을 수 없어요", 남의 일지면 FORBIDDEN (문구는 API마다)

### journal/service/AuthorLabels
- 익명 작성자 표시. `sameCohort`(14기 · PM, 검토 화면도 이것), `pastCohort`(지난 기수 B팀 · PM, 미분류면 팀 생략), 운영진은 "14기 운영진"/"지난 기수 운영진", 파트 없으면 생략
- `preview`·`audience`: 4번 미리보기 (NOW "지금 14기에게 (같은 팀원 제외)", NEXT "다음 기수(15기)부터")
- 팀 가명은 지금 `ProjectCategory.teamLetter`("B" → "B팀"). ERD 이름은 anon_label

### journal/ai/AiClient
- AI 호출은 이 인터페이스로만. 실패는 `AiClientException`
- 구현 선택: `app.ai.api-key`(`AI_API_KEY`)가 있으면 `GeminiAiClient`, 비어 있으면 `FakeAiClient`
- `GeminiAiClient`: `generateContent`를 JSON 응답 모드(`responseSchema`)로 호출. 모델은 `app.ai.model`(`AI_MODEL`, 기본 gemini-3.5-flash-lite). 연결 5초·응답 30초 제한. 목록에 없는 태그는 버림(최대 3개), 원문이 빈 칸(again·memo)은 정리본도 비움, 원문은 로그에 안 남김
- `GeminiPrompts`: 지시문 (이름 → `⟦팀원⟧`, 내용 지어내지 않기, 제목 한 줄, 태그 1~3개, 보완 질문은 처음만)
- `FakeAiClient`: 고정 제목·태그·보완 질문, 정리본 = 원문 그대로(이름 안 가림)
- 테스트는 `build.gradle`에서 `AI_API_KEY`를 비워서 항상 Fake (PC에 키가 있어도 실제 호출 안 함)

### journal/controller/JournalController
| API | 화면 |
|---|---|
| `GET /api/journals/form` | SCR-JOUR-01 성장일지 쓰기 (기본값) |
| `POST /api/journals` | SCR-JOUR-01 첫 입력 (임시 저장, id 발급) |
| `PUT /api/journals/{id}` | SCR-JOUR-01 자동 저장 (입력 멈추고 3초 뒤) |
| `POST /api/journals/{id}/summary` | SCR-JOUR-02 AI로 정리하기 |
| `POST /api/journals/{id}/submit` body `{tags}` | SCR-JOUR-02 검토 요청 |
| `GET /api/journals/me` | 내 기록 탭 (통계 + 목록) |
| `GET /api/journals/{id}` | 내 기록 → 일지 열기 / 이어 쓰기 |

### global/exception/GlobalExceptionHandler (B가 추가)
- `HttpMessageNotReadableException`(깨진 JSON, 값 목록에 없는 enum), `MethodArgumentTypeMismatchException`(경로 변수 타입) → 400 INVALID_INPUT. 전에는 500으로 나갔음

---

## note/ (B)

### journal/repository/JournalRepository — 노트 공개 조건 (`VISIBLE_NOTE`)
- **익명성 규칙의 핵심. 노트를 보여주는 곳은 전부 `findVisibleNotes` / `findVisibleNote`로만 가져오기**
- APPROVED 이고 (내 노트) 또는 (지난 기수) 또는 (같은 기수 + NOW + 미분류이거나 내가 그 프로젝트에 배정된 적 없음 — `ProjectAssignment`를 `not exists`로, 지난 배정 포함)
- 같은 기수 NEXT, 다음 기수, 같은(지난) 팀원 노트는 안 나옴

### note/service/NoteService
- `list` (8번): 공개 노트 전부 → 서버에서 cohort·part·timing·tag 필터 + 검색 → 정렬(NEW = 공개 시각 `reviewedAt` 최신, READS·HELPS = 많은 순 → 최신) → 페이징(size 1~50). 읽음·도움돼요 수는 쿼리 한 번(`countByJournalIds`)
- `detail` (9번): 볼 수 없으면 404 "노트를 찾을 수 없어요"(없음·미승인과 구분 안 함). 처음 열면 `NoteReaction` 생성(작성자 제외). `similarNotes` 최대 2개 = 볼 수 있는 노트 중 점수(겹치는 태그×2 + 같은 파트 1 + 같은 시점 1) > 0, 같으면 도움돼요 순
- `toggleHelp` (10번): 404 → 내 노트면 403 → 행 없으면 만들고(읽음도 생김) `helped` 뒤집기
- `authorLabel`: 같은 기수(내 노트 포함)면 `AuthorLabels.sameCohort`, 지난 기수면 `pastCohort`(팀 가명)

### note/service/NoteSearch
- 검색어: 공백 빼고 비교, 여러 단어 모두 포함, `#`로 시작하면 태그(화면 이름·값). 대상 = 제목 + 정리본 4칸 + 태그 화면 이름. **authorLabel·팀 가명·프로젝트 이름은 대상 아님**
- 미리보기: 정리본 "넘긴 방법" 앞 80자 + "…", 없으면 "막힌 것"

### note/controller/NoteController
| API | 화면 |
|---|---|
| `GET /api/notes?q=&cohort=&part=&timing=&tag=&sort=&page=&size=` | 노트 게시판. 파트 A 회의 도우미 "관련 노트"는 `?tag=MEETING_RUN&size=2` 등으로 |
| `GET /api/notes/{id}` | 노트 상세 |
| `POST /api/notes/{id}/help` | 도움돼요 토글 |

### note/dto/NoteCard
- 8번 카드, 9번 similarNotes, 회의 도우미 관련 노트가 같은 모양. 작성자 id·이름·projectId 없음

---

## 운영진 검토 (B, journal/)

### journal/controller/AdminReviewController
- 모든 메서드가 `adminChecker.require(userId)`로 시작 (운영진 아니면 FORBIDDEN "운영진만 할 수 있어요")

| API | 화면 |
|---|---|
| `GET /api/admin/reviews` | 검토 탭 (pendingCount, pending, recent) |
| `GET /api/admin/reviews/{journalId}` | 검토 상세 |
| `POST /api/admin/reviews/{journalId}/approve` | 승인 |
| `POST /api/admin/reviews/{journalId}/reject` body `{reason}` | 돌려보내기 |

### journal/service/ReviewService
- 운영진은 **자기 기수 일지만** (운영진 기수 = 아기사자 기수). 다른 기수·임시 저장·없는 일지는 404 "일지를 찾을 수 없어요"
- `list`: 같은 기수 PENDING `updatedAt` 오래된 순, 본인 일지 제외(그 기수 운영진 1명이면 포함). `recent` = 같은 기수에서 승인·돌려보낸 것 `reviewedAt` 최신 10개 (운영진 전체)
- `detail`: 공개될 정리본 그대로(원문 X), `authorLabel` = `AuthorLabels.sameCohort`(팀 없음). 처리된 일지도 열람 가능
- `approve`·`reject`: 404 → 본인 일지 403 "내 일지는 검토할 수 없어요"(운영진 1명이면 허용) → `Journal.approve/reject`(PENDING 아니면 400, 사유 1~200자). 검토자·처리 시각 기록
- 운영진 1명 판단: `userRepository.countByCohortAndType(cohort, ADMIN) <= 1`

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
| app.today | 오늘 날짜 고정 (비우면 실제 오늘) | `APP_TODAY` |

---

## 테스트

| 파일 | 확인하는 것 |
|---|---|
| auth/AuthFlowTest | 가짜 구글 토큰 → NEED_SIGNUP → 틀린 코드 400 → 가입 → JWT로 /api/me → 재로그인 REGISTERED → 토큰 없이 401 |
| admin/AdminFlowTest | 운영진 가입 → 프로젝트(팀 글자 A·B, 운영 null) → 아기사자 가입 → 기본 파트 → 배정 → 아기사자 홈에 PM 할 일. 아기사자의 운영진 API 403. 일정 등록, PROJECT_START 중복 400 |
| meeting/MeetingTest | 템플릿 4종, 빈 줄 제거·공유 원문, 최근 7일 필터, 남의 회의 404, 삭제, 둘 다 비면 400 |
| guide/HomeGuideTest | 2주차·D-12·W1_2 계산, 할 일 체크/해제 반영, 배정 전 빈 할 일, 운영진 403, 다른 파트·주차 가이드 |
| user/AssignmentServiceTest | 기본 파트 복사, 재배정 이력, 배정 해제, 파트 없음 에러, 운영 분류는 OPS |
| journal/JournalFormTest | 지금 배정이 기본값·지난 배정도 목록에, 배정 전 미분류+기본 파트, 일정 없는 기수 시점 null, 401 |
| journal/JournalWriteTest | 일지 생성, 빈 body 미분류, 남의 프로젝트 400, 잘못된 enum 400, 2000자 초과 400, 401 |
| journal/JournalAutoSaveTest | 전체 덮어쓰기, AI 정리본 유지, PENDING 수정 400, 남의 일지 403, 없는 일지 404, id 타입 400 |
| journal/FakeAiClientTest | 키 없으면 FakeAiClient 주입, 고정 응답, 보완 질문은 처음만 |
| journal/JournalSummaryTest | 정리·미리보기, 보완 질문 처음만, 팀 가명·미분류, 운영진·파트 없음 표시, 필수 조건 3종 400, PENDING 400, 403/404 |
| journal/JournalSummaryAiFailTest | AI 실패 시 500, 원문 유지 (AiClient를 MockitoBean으로) |
| journal/ai/GeminiAiClientTest | 가짜 서버로 요청 헤더·응답 해석, 이상한 태그 버림, 빈 칸 유지, 보완 질문, 실패 3종 → AiClientException |
| journal/ai/AiClientSelectionTest | 키가 있으면 GeminiAiClient 주입 |
| journal/JournalMineTest | 상태별 개수·최근 수정 순·정리 전 title null·미분류 projectTitle null, 반응은 내 공개 노트만, journalBanner·noteImpact, 401 |
| journal/JournalDetailTest | 원문·정리본·사유·projectId, 정리 전 summary null, 403/404, /me·/form이 상세로 안 잡힘 |
| note/NoteListTest | 공개 조건(지금·지난 팀원, 같은 기수 NEXT, 다음 기수, 미승인 숨김), 팀 가명은 지난 기수만, 팀 이름 검색 안 됨, 띄어쓰기 무시·여러 단어·#태그, 필터·정렬·isRead·페이징·400 |
| note/NoteDetailTest | 정리본·라벨, 읽음 1회·작성자 제외, 숨김 대상 전부 404(기록도 안 남음), similarNotes 점수순·숨김 노트 제외 |
| note/NoteHelpTest | 토글·개수, 바로 눌러도 읽음 생김, 내 노트 403, 팀원 노트 404 |
| note/NoteFixtures | 노트 테스트 데이터 도우미 (승인된 노트, 반응) |
| journal/ReviewListTest | 같은 기수만·오래된 순·본인 제외, 운영진 1명이면 본인 포함, recent는 운영진 전체·최신순, 아기사자 403 |
| journal/ReviewDetailTest | 정리본만(원문·작성자 없음)·팀 없는 라벨, 처리된 일지 열람, 임시 저장·다른 기수 404, 아기사자 403 |
| journal/ReviewApproveTest | 승인 → 검토자·시각 기록·노트 게시판에 보임, 이미 처리 400, 본인 403·운영진 1명이면 허용, 다른 기수 404 |
| journal/ReviewRejectTest | 사유 저장(앞뒤 공백 제거)·작성자 내 일지에서 보임, 빈 값·201자 400·200자 OK, 이미 처리 400, 본인 403, 다른 기수 404 |
| journal/JournalSubmitTest | 태그 빼기·전부 빼기, 새 태그 400, 정리본 없음 400, 중복 요청 400, 돌려받은 사유 지움, tags 없음 400, 403 |
