# API 명세서

- 원본은 노션(멋사수첩 > API 명세서). 바뀌면 노션과 이 파일을 같이 고치기
- **A(언니)**: 인증·구글 로그인·초대코드, 멤버·프로젝트 배정, 홈·가이드·일정, 회의 → 언니 노션 명세 참고 (아래에 옮겨 오면 좋음)
- **B(나)**: 성장일지, AI 정리, 노트, 운영진 검토, 포트폴리오 → 이 파일

## 공통 규칙

| 항목 | 규칙 |
|---|---|
| 인증 | `Authorization: Bearer {accessToken}`. local에서만 `X-User-Id` 헤더로 대신(언니가 구현). 배포 서버에서는 꺼짐 |
| 성공 응답 | `{ "success": true, "data": { ... } }` |
| 실패 응답 | `{ "success": false, "code": "NOT_FOUND", "message": "..." }` |
| 에러 코드 | `INVALID_INPUT`(400) · `UNAUTHORIZED`(401) · `FORBIDDEN`(403) · `NOT_FOUND`(404) · `INTERNAL_ERROR`(500) 5개만. 상황은 message로 구분 |
| 날짜 | 날짜만 `YYYY-MM-DD`, 시간까지 `YYYY-MM-DDTHH:mm:ss` (KST) |
| 페이징 | 노트 목록만. `page`(0부터), `size`(기본 20, 최대 50). 나머지 목록은 페이징 없음 |
| JSON 키 | camelCase |
| 값 | 전부 대문자 (DB도 대문자로 저장) |
| 익명성 | 노트·검토 응답에 작성자 id·이름·projectId 없음. 작성자 표시는 `authorLabel` 문자열 하나 |
| 운영진 권한 | `/api/admin/**`는 A의 공통 체크(`AdminChecker`) 한 곳에서. 운영진 아니면 `403 FORBIDDEN` "운영진만 할 수 있어요" |

### 값 목록

| 이름 | 값 |
|---|---|
| part | `PM` · `DESIGN` · `FRONTEND` · `BACKEND` · `OPS`(운영진) |
| activity | `SESSION` · `IDEATHON` · `HACKATHON` · `MIDTERM` · `DEMODAY` · `ETC` |
| timing | `W2PLUS`(2주 이상 전) · `W1_2`(1~2주 전) · `DURING`(행사 주간) · `AFTER`(행사 후) |
| scope | `NOW`(지금 같은 기수에게, 같은 팀원 제외) · `NEXT`(다음 기수부터) |
| status | `DRAFT`(임시 저장) · `PENDING`(검토 대기) · `REJECTED`(돌려받음) · `APPROVED`(공개) |
| tags | 최대 3개. 아래 12개 중에서 |
| format | `RESUME_3LINES`(이력서 3줄) · `EXPERIENCE_PARAGRAPH`(경험 서술 문단) |

### 상황 태그 (12개)

AI 정리 때 일지 내용을 보고 붙이는 "어떤 상황이었나" 라벨. 노트 게시판 "상황" 필터와 `#태그` 검색에 씀.

| 값 | 화면 표시 | 값 | 화면 표시 |
|---|---|---|---|
| `MEETING_RUN` | 회의 진행 | `PLAN_DEV` | 기획↔개발 전달 |
| `CONFLICT` | 의견 충돌 | `DESIGN_COLLAB` | 디자인 협업 |
| `TOPIC_PICK` | 주제 선정 | `STACK` | 기술 스택 선택 |
| `TOPIC_CHANGE` | 주제 변경 | `RELEASE` | 발표·배포 준비 |
| `DELAY` | 일정 지연 | `ROLE_SPLIT` | 역할 분담 |
| `PLAN_DOC` | 기획서 작성 | `PARTICIPATION` | 팀원 참여도 |

### authorLabel 만드는 법

| 경우 | 예시 |
|---|---|
| 같은 기수 노트 | `14기 · PM` (파트는 `journal.part`) |
| 지난 기수 노트 | `지난 기수 B팀 · PM` (팀 가명은 `project_category.anon_label`) |
| 지난 기수 + 미분류 일지 | `지난 기수 · PM` |
| 운영진 글 | `14기 운영진` / `지난 기수 운영진` |
| 검토 화면 | 공개 범위와 상관없이 팀 빼고 `14기 · PM`만 (운영진은 팀 배정을 알아서 팀+파트면 작성자를 특정할 수 있음) |

---

## 성장일지

### 1. 작성 화면 기본값

`GET /api/journals/form`

| 항목 | 내용 |
|---|---|
| 화면 | 성장일지 작성 (새로 쓰기) |
| 인증 | 로그인 |
| 설명 | 고를 수 있는 프로젝트와 기본값(프로젝트·파트·시점)을 줌 |

**Response**

```json
{
  "success": true,
  "data": {
    "projects": [
      { "id": 3, "title": "14기 중앙해커톤 · 멋사수첩팀", "part": "BACKEND" }
    ],
    "defaultProjectId": 3,
    "defaultPart": "BACKEND",
    "defaultTiming": "W1_2"
  }
}
```

**규칙**

- `projects` = 내가 `project_member`로 들어 있는 프로젝트. 운영진은 `OPERATION` 분류도 포함
- `defaultProjectId` = `is_current = true`인 프로젝트. 없으면 `null`(미분류)
- `defaultPart` = 그 프로젝트의 `project_member.part`. 없으면 `users.part`
- `defaultTiming` = A 홈과 같은 계산(`StageService`): 다음 큰 행사까지 14일 이상 `W2PLUS`, 7~13일 `W1_2`, 0~6일 `DURING`, 남은 행사 없으면 `AFTER`. 그 기수 일정이 아예 없으면 `null`
- `activity`는 기본값 없음 (직접 고르기)

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 401 | UNAUTHORIZED | 로그인이 필요해요 | 토큰 없음·만료 |

---

### 2. 일지 새로 만들기 (임시 저장)

`POST /api/journals`

| 항목 | 내용 |
|---|---|
| 화면 | 성장일지 작성 — 처음 입력을 시작할 때 |
| 인증 | 로그인 |
| 설명 | `DRAFT` 상태 일지를 만들고 id를 돌려줌. 이후 저장은 3번(PUT) |

**Request**

```json
{
  "projectId": 3,
  "part": "BACKEND",
  "activity": "HACKATHON",
  "timing": "W1_2",
  "scope": "NOW",
  "rawStuck": "",
  "rawSolved": "",
  "rawAgain": "",
  "rawMemo": ""
}
```

| 필드 | 필수 | 규칙 |
|---|---|---|
| projectId | X | `null`이면 미분류. 값이 있으면 내가 팀원인 프로젝트여야 함 |
| part | X | part 값 중 하나 |
| activity · timing · scope | X | 값 목록 중 하나 (임시 저장이라 비어도 됨) |
| rawStuck · rawSolved · rawAgain · rawMemo | X | 각 2000자 이하 |

**Response**

```json
{
  "success": true,
  "data": { "id": 12, "status": "DRAFT", "updatedAt": "2026-11-12T17:30:00" }
}
```

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 400 | INVALID_INPUT | 입력값을 확인해 주세요 | 값 목록에 없는 값, 글자 수 초과, 내 프로젝트가 아님 |
| 401 | UNAUTHORIZED | 로그인이 필요해요 | |

---

### 3. 일지 저장 (자동 저장)

`PUT /api/journals/{id}`

| 항목 | 내용 |
|---|---|
| 화면 | 성장일지 작성 — 자동 저장 |
| 인증 | 로그인 (작성자 본인) |
| 설명 | 보낸 값으로 덮어씀. Request 형식은 2번과 같음 |

**규칙**

- `DRAFT`, `REJECTED`일 때만 수정 가능
- 보낸 body 전체로 덮어씀 (안 보낸 칸은 `null`)
- AI 정리본(`summaryTitle`, `sum*`)은 지우지 않음. 다시 정리(4번)할 때 덮어씀

**프론트 처리**

- 자동 저장: 입력이 멈추고 3초 뒤에 저장 (계속 치는 중에는 안 보냄)
- 첫 입력 때 2번(POST)으로 id를 받고, 그다음부터 3번(PUT)

**Response**: 2번과 같음

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 400 | INVALID_INPUT | 지금은 수정할 수 없어요 | `PENDING`·`APPROVED` 상태 |
| 400 | INVALID_INPUT | 입력값을 확인해 주세요 | 값·글자 수 문제 |
| 403 | FORBIDDEN | 내 일지만 수정할 수 있어요 | 다른 사람 일지 |
| 404 | NOT_FOUND | 일지를 찾을 수 없어요 | 없는 id |

---

### 4. AI 정리

`POST /api/journals/{id}/summary`

| 항목 | 내용 |
|---|---|
| 화면 | 성장일지 작성 → "정리하기" |
| 인증 | 로그인 (작성자 본인) |
| 설명 | 저장된 원문으로 제목·태그·이름 가린 정리본을 만들어 저장하고 돌려줌. Body 없음 |

**정리 전 필수 조건** (안 맞으면 400)

| 필드 | 규칙 |
|---|---|
| activity · timing · scope | 값 있어야 함 |
| rawStuck | 30자 이상 |
| rawSolved | 비어 있으면 안 됨 (노트에서 제일 쓸모 있는 칸이라 필수) |

**Response**

```json
{
  "success": true,
  "data": {
    "title": "첫 회의에서 역할이 안 정해졌을 때",
    "tags": ["MEETING_RUN", "ROLE_SPLIT"],
    "body": {
      "stuck": "⟦팀원⟧이 맡을 일이 정해지지 않아...",
      "solved": "...",
      "again": "...",
      "memo": null
    },
    "authorLabel": "14기 · BACKEND",
    "audience": "지금 14기에게 (같은 팀원 제외)",
    "followUpQuestion": "그때 역할은 누가 어떻게 나눴나요?"
  }
}
```

**규칙**

- 사람 이름은 `⟦팀원⟧`처럼 가려서 저장 (`sum_*`)
- `tags`는 12개 목록 중 최대 3개
- `followUpQuestion`(보완 질문): **처음 정리할 때(`summary_title`이 비어 있을 때)만** "넘긴 방법" 관련으로 1개 생성. 두 번째부터는 `null`. DB에 저장 안 함
- `authorLabel`, `audience`는 공개되면 이렇게 보인다는 미리보기

**프론트 처리**

- 보완 질문 답은 `rawSolved` 끝에 붙여서 3번(PUT)으로 저장 → 다시 이 API 호출. 건너뛰어도 됨
- AI 실패해도 원문은 이미 저장돼 있으니 "다시 시도" 버튼만 보여주기

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 400 | INVALID_INPUT | 막힌 것을 30자 이상 써 주세요 | 필수 조건 미충족 (message는 상황별) |
| 400 | INVALID_INPUT | 지금은 정리할 수 없어요 | `PENDING`·`APPROVED` 상태 |
| 403 | FORBIDDEN | 내 일지만 정리할 수 있어요 | |
| 404 | NOT_FOUND | 일지를 찾을 수 없어요 | |
| 500 | INTERNAL_ERROR | 정리에 실패했어요. 잠시 후 다시 시도해 주세요 | AI 호출 실패 |

**AI 연동**: 서버는 `AiClient` 인터페이스로만 부름. `AI_API_KEY` 환경변수가 있으면 Gemini, 없으면 `FakeAiClient`(고정 응답, 이름 가리기 안 함). 응답 형식은 같음

---

### 5. 검토 요청

`POST /api/journals/{id}/submit`

| 항목 | 내용 |
|---|---|
| 화면 | AI 정리 결과 → "검토 요청" |
| 인증 | 로그인 (작성자 본인) |
| 설명 | 태그를 최종 확인하고 `PENDING`으로 바꿈 |

**Request**

```json
{ "tags": ["MEETING_RUN"] }
```

| 필드 | 필수 | 규칙 |
|---|---|---|
| tags | O | AI가 준 태그 중에서 **빼기만** 가능 (새로 추가 X). 0개도 됨 |

**Response**

```json
{
  "success": true,
  "data": { "id": 12, "status": "PENDING", "updatedAt": "2026-11-12T17:40:00" }
}
```

**규칙**

- `DRAFT`·`REJECTED` + 정리본이 있을 때만
- 돌려받은 일지를 다시 요청하면 `reject_reason`은 지움
- 검토 대기열 정렬은 `updated_at` 기준 (`submitted_at` 컬럼 없음)

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 400 | INVALID_INPUT | 먼저 AI 정리를 해 주세요 | 정리본 없음 |
| 400 | INVALID_INPUT | 태그는 정리된 것 중에서만 고를 수 있어요 | 새 태그 추가 |
| 400 | INVALID_INPUT | 이미 검토 요청한 일지예요 | `PENDING`·`APPROVED` |
| 403 | FORBIDDEN | 내 일지만 요청할 수 있어요 | |
| 404 | NOT_FOUND | 일지를 찾을 수 없어요 | |

---

### 6. 내 기록

`GET /api/journals/me`

| 항목 | 내용 |
|---|---|
| 화면 | 내 기록 탭 |
| 인증 | 로그인 |
| 설명 | 내 일지 통계 + 목록 (최신순, 페이징 없음) |

**Response**

```json
{
  "success": true,
  "data": {
    "stats": {
      "total": 5, "approved": 3, "pending": 1, "rejected": 0, "draft": 1,
      "readCount": 42, "helpCount": 7
    },
    "journals": [
      {
        "id": 12,
        "title": "첫 회의에서 역할이 안 정해졌을 때",
        "status": "PENDING",
        "activity": "HACKATHON",
        "timing": "W1_2",
        "projectTitle": "14기 중앙해커톤 · 멋사수첩팀",
        "rejectReason": null,
        "updatedAt": "2026-11-12T17:40:00"
      }
    ]
  }
}
```

**규칙**

- 본인 것이라 `projectTitle`을 줘도 됨. 미분류면 `null`
- `title`은 정리 전이면 `null` → 프론트에서 "제목 없음"
- `readCount`, `helpCount` = 내 공개 노트들의 `note_reaction` 합계

---

### 7. 내 일지 상세

`GET /api/journals/{id}`

| 항목 | 내용 |
|---|---|
| 화면 | 내 기록 → 일지 열기 / 이어 쓰기 |
| 인증 | 로그인 (작성자 본인만) |
| 설명 | 원문 + 정리본 전부 |

**Response**

```json
{
  "success": true,
  "data": {
    "id": 12,
    "status": "REJECTED",
    "projectId": 3,
    "part": "BACKEND",
    "activity": "HACKATHON",
    "timing": "W1_2",
    "scope": "NOW",
    "raw": { "stuck": "...", "solved": "...", "again": "...", "memo": null },
    "summary": {
      "title": "...",
      "tags": ["MEETING_RUN"],
      "body": { "stuck": "...", "solved": "...", "again": "...", "memo": null }
    },
    "rejectReason": "팀 이름이 그대로 들어가 있어요",
    "updatedAt": "2026-11-12T17:40:00"
  }
}
```

- 정리 전이면 `summary`는 `null`

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 403 | FORBIDDEN | 내 일지만 볼 수 있어요 | |
| 404 | NOT_FOUND | 일지를 찾을 수 없어요 | |

---

## 노트

### 8. 노트 목록·검색

`GET /api/notes`

| 항목 | 내용 |
|---|---|
| 화면 | 노트 게시판 |
| 인증 | 로그인 |
| 설명 | 내가 볼 수 있는 승인된 노트 목록. 필터·검색·정렬·페이징 |

**Request (쿼리)**

| 필드 | 필수 | 규칙 |
|---|---|---|
| q | X | 검색어. 띄어쓰기 무시, 여러 단어는 모두 포함. `#역할 분담`처럼 `#`로 시작하면 태그 검색 |
| cohort | X | `ALL`(기본) · `PAST`(지난 기수) · `NOW`(같은 기수) |
| part | X | part 값 |
| timing | X | timing 값 |
| tag | X | 태그 값 1개 (예: `ROLE_SPLIT`) |
| sort | X | `NEW`(기본, 최신순) · `READS`(많이 읽은 순) · `HELPS`(도움돼요 순) |
| page · size | X | 공통 규칙 |

예: `GET /api/notes?cohort=PAST&part=PM&sort=HELPS&page=0&size=20`

**Response**

```json
{
  "success": true,
  "data": {
    "notes": [
      {
        "id": 7,
        "title": "첫 회의에서 역할이 안 정해졌을 때",
        "authorLabel": "지난 기수 B팀 · PM",
        "isCurrentCohort": false,
        "isMine": false,
        "activity": "HACKATHON",
        "timing": "W1_2",
        "tags": ["MEETING_RUN"],
        "preview": "역할을 먼저 나누지 않고...",
        "previewFrom": "SOLVED",
        "readCount": 31,
        "helpCount": 6,
        "isRead": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalCount": 48,
    "hasNext": true
  }
}
```

**보이는 조건 (서버 쿼리에서 거름)**

- `status = APPROVED`
- 그리고 다음 중 하나
    - 작성자 기수 < 내 기수 (지난 기수 노트, scope 상관없음)
    - 같은 기수 + `scope = NOW` + 내가 **그 일지 프로젝트**의 `project_member`가 아님 (`is_current = false` 행 포함. 미분류 일지는 숨기지 않음)
    - 내가 쓴 노트 (`isMine = true`, 항상 보임)

**규칙**

- `preview`: 정리본 "넘긴 방법" 앞부분 (없으면 "막힌 것"). `previewFrom`은 `SOLVED` · `STUCK`
- 검색 대상: 제목 + 정리본 본문 + 태그(화면 표시 이름). `authorLabel`(팀 가명)은 검색 안 함 → 같은 기수 노트가 팀으로 걸리지 않게
- 노트 카드 모양은 A의 회의 도우미 "관련 노트"에서도 같이 씀

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 400 | INVALID_INPUT | 입력값을 확인해 주세요 | 값 목록에 없는 필터, size > 50 |
| 401 | UNAUTHORIZED | 로그인이 필요해요 | |

---

### 9. 노트 상세

`GET /api/notes/{id}`

| 항목 | 내용 |
|---|---|
| 화면 | 노트 상세 |
| 인증 | 로그인 |
| 설명 | 정리본 전체 + 비슷한 노트 2개. 열면 읽음 기록 |

**Response**

```json
{
  "success": true,
  "data": {
    "id": 7,
    "title": "첫 회의에서 역할이 안 정해졌을 때",
    "authorLabel": "지난 기수 B팀 · PM",
    "isCurrentCohort": false,
    "isMine": false,
    "activity": "HACKATHON",
    "timing": "W1_2",
    "tags": ["MEETING_RUN"],
    "body": { "stuck": "...", "solved": "...", "again": "...", "memo": null },
    "readCount": 32,
    "helpCount": 6,
    "helped": false,
    "similarNotes": [ { "...": "8번 노트 카드와 같은 모양" } ]
  }
}
```

**규칙**

- 보이는 조건은 8번과 같음. 볼 수 없는 노트는 **404** (있다는 것도 알려주지 않음)
- 처음 열면 `note_reaction` 행 생성(읽음 1인 1회). 작성자 본인은 기록 안 함
- `similarNotes`: 같은 태그·파트·시점 중 볼 수 있는 노트 최대 2개

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 404 | NOT_FOUND | 노트를 찾을 수 없어요 | 없음·미승인·숨김 대상 |

---

### 10. 도움돼요

`POST /api/notes/{id}/help`

| 항목 | 내용 |
|---|---|
| 화면 | 노트 상세 → "도움돼요" |
| 인증 | 로그인 |
| 설명 | 누를 때마다 켜짐/꺼짐 (토글). Body 없음 |

**Response**

```json
{ "success": true, "data": { "helped": true, "helpCount": 7 } }
```

**규칙**

- `note_reaction.helped`만 바꿈 (행 없으면 만들고 true)
- 도움돼요 알림은 이번 시연에서 제외

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 403 | FORBIDDEN | 내 노트에는 누를 수 없어요 | 작성자 본인 |
| 404 | NOT_FOUND | 노트를 찾을 수 없어요 | 9번과 같음 |

---

## 운영진 검토

### 11. 검토 목록

`GET /api/admin/reviews`

| 항목 | 내용 |
|---|---|
| 화면 | 운영진 — 검토 탭 |
| 인증 | 운영진(`ADMIN`) |
| 설명 | 검토 대기(오래된 순) + 최근 처리한 것 |

**Response**

```json
{
  "success": true,
  "data": {
    "pendingCount": 1,
    "pending": [
      {
        "journalId": 12,
        "title": "첫 회의에서 역할이 안 정해졌을 때",
        "authorLabel": "14기 · BACKEND",
        "activity": "HACKATHON",
        "timing": "W1_2",
        "scope": "NOW",
        "requestedAt": "2026-11-12T17:40:00"
      }
    ],
    "recent": [
      { "journalId": 9, "title": "...", "status": "APPROVED", "reviewedAt": "2026-11-11T20:00:00" }
    ]
  }
}
```

**규칙**

- `pendingCount`: 검토 대기 개수 (`pending` 개수와 같음). 검토 탭 위 숫자. 같은 줄의 미배정·다음 행사 숫자는 프론트가 A의 members·events API에서 가져감
- `pending`: `PENDING`, `updated_at` 오래된 순. `requestedAt` = `updated_at`
- 본인 일지는 빼기. 단 운영진이 1명뿐이면 포함
- `recent`: `reviewed_at` 최신순 10개
- 작성자 id·이름·팀 없음

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 401 | UNAUTHORIZED | 로그인이 필요해요 | |
| 403 | FORBIDDEN | 운영진만 볼 수 있어요 | `MEMBER` |

---

### 12. 검토 상세

`GET /api/admin/reviews/{journalId}`

| 항목 | 내용 |
|---|---|
| 화면 | 검토 탭 → 일지 열기 |
| 인증 | 운영진 |
| 설명 | 공개될 정리본 그대로 보여줌 (원문 X) |

**Response**

```json
{
  "success": true,
  "data": {
    "journalId": 12,
    "status": "PENDING",
    "title": "...",
    "tags": ["MEETING_RUN"],
    "body": { "stuck": "...", "solved": "...", "again": "...", "memo": null },
    "authorLabel": "14기 · BACKEND",
    "activity": "HACKATHON",
    "timing": "W1_2",
    "scope": "NOW",
    "requestedAt": "2026-11-12T17:40:00"
  }
}
```

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 403 | FORBIDDEN | 운영진만 볼 수 있어요 | |
| 404 | NOT_FOUND | 일지를 찾을 수 없어요 | 없음, `DRAFT` 상태 |

---

### 13. 승인

`POST /api/admin/reviews/{journalId}/approve`

| 항목 | 내용 |
|---|---|
| 화면 | 검토 상세 → "승인" |
| 인증 | 운영진 |
| 설명 | `APPROVED`로 바꾸고 노트로 공개. Body 없음 |

**Response**

```json
{ "success": true, "data": { "journalId": 12, "status": "APPROVED", "reviewedAt": "2026-11-12T18:00:00" } }
```

- `reviewer_id`, `reviewed_at` 기록
- 공개 취소는 이번 시연에서 제외

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 400 | INVALID_INPUT | 이미 처리된 일지예요 | `PENDING`이 아님 |
| 403 | FORBIDDEN | 운영진만 할 수 있어요 | |
| 403 | FORBIDDEN | 내 일지는 검토할 수 없어요 | 본인 일지 (운영진 1명이면 허용) |
| 404 | NOT_FOUND | 일지를 찾을 수 없어요 | |

---

### 14. 돌려보내기

`POST /api/admin/reviews/{journalId}/reject`

| 항목 | 내용 |
|---|---|
| 화면 | 검토 상세 → "돌려보내기" |
| 인증 | 운영진 |
| 설명 | `REJECTED`로 바꾸고 사유 저장. 작성자는 내 기록에서 사유를 보고 고쳐서 다시 요청 |

**Request**

```json
{ "reason": "팀 이름이 그대로 들어가 있어요" }
```

| 필드 | 필수 | 규칙 |
|---|---|---|
| reason | O | 1~200자 |

**Response**

```json
{ "success": true, "data": { "journalId": 12, "status": "REJECTED", "reviewedAt": "2026-11-12T18:00:00" } }
```

**에러**: 13번과 같음 + `400 INVALID_INPUT` "사유를 200자 이내로 써 주세요" (빈 값·초과)

---

## 포트폴리오

### 15. 포트폴리오 프로젝트 목록

`GET /api/portfolio/projects`

| 항목 | 내용 |
|---|---|
| 화면 | 포트폴리오 요약 |
| 인증 | 로그인 |
| 설명 | 요약 만들 수 있는 내 프로젝트 + 오늘 남은 횟수 |

**Response**

```json
{
  "success": true,
  "data": {
    "projects": [
      { "projectId": 3, "title": "14기 중앙해커톤 · 멋사수첩팀", "journalCount": 4, "available": true }
    ],
    "remainingToday": 5
  }
}
```

**규칙**

- 내가 일지를 쓴 프로젝트만. 미분류 제외
- `journalCount` = 임시 저장(`DRAFT`) 뺀 내 일지 수. 2개 이상이면 `available = true`
- `remainingToday` = 5 − 오늘(KST) 내 `portfolio_summary` 개수

---

### 16. 포트폴리오 요약 만들기

`POST /api/portfolio/summaries`

| 항목 | 내용 |
|---|---|
| 화면 | 포트폴리오 요약 → "만들기" |
| 인증 | 로그인 |
| 설명 | 그 프로젝트의 내 일지로 AI가 요약을 만들고 `portfolio_summary`에 저장 |

**Request**

```json
{ "projectId": 3, "format": "RESUME_3LINES" }
```

| 필드 | 필수 | 규칙 |
|---|---|---|
| projectId | O | 15번에서 `available = true`인 프로젝트 |
| format | O | `RESUME_3LINES` · `EXPERIENCE_PARAGRAPH` |

**Response**

```json
{
  "success": true,
  "data": {
    "id": 21,
    "format": "RESUME_3LINES",
    "result": "- 해커톤 팀에서 백엔드를 맡아 ⟪숫자⟫개 API를 ...",
    "sourceJournalIds": [10, 11, 12],
    "remainingToday": 4,
    "createdAt": "2026-11-12T18:10:00"
  }
}
```

**규칙**

- 근거는 그 프로젝트의 내 일지(`DRAFT` 제외, 검토 상태 무관) 원문
- 직접 채워야 할 숫자·결과는 `⟪⟫` 빈칸으로 남김

**에러**

| HTTP | code | message | 상황 |
|---|---|---|---|
| 400 | INVALID_INPUT | 오늘은 다 만들었어요. 내일 다시 해 주세요 | 하루 5회 초과 |
| 400 | INVALID_INPUT | 일지가 2개 이상 있어야 해요 | 일지 부족 |
| 400 | INVALID_INPUT | 입력값을 확인해 주세요 | format 잘못됨, 내 프로젝트 아님 |
| 500 | INTERNAL_ERROR | 요약에 실패했어요. 잠시 후 다시 시도해 주세요 | AI 실패 (횟수 차감 안 함) |

---

## 시연 제외

- 추천 노트, 승인된 노트 공개 취소·삭제, 도움돼요 알림

## A와 연결되는 부분

- 노트 카드 모양(8번)을 회의 도우미 "관련 노트"에서도 씀
- 명세의 "분류가 회의인 노트"는 분류 컬럼을 빼서 태그 `MEETING_RUN`으로 대신 (`GET /api/notes?tag=MEETING_RUN&size=2`)
- 아기사자 홈(`GET /api/home`, A)에 B 필드 2개. A의 `HomeService`가 B 서비스 메서드를 호출 (B는 API 추가 없이 메서드만 준비)
  - `journalBanner`: 내 일지 중 돌려받음(`REJECTED`)·검토 중(`PENDING`) 개수
  - `noteImpact`: 내 공개 노트(`APPROVED`)의 읽음·도움돼요 합계 (`note_reaction`)
- 회의 불러오기(일지 작성 화면): 프론트가 A의 `GET /api/meetings?days=7`을 호출. B는 API 추가 없음
- 운영 분류(`OPERATION`) 배정의 파트는 `OPS` (ERD 코멘트의 NULL 대신)
- `project_member.part`, `is_current`, `anon_label`은 A가 배정할 때 채워줘야 B 쪽 기본값·표시가 맞음

