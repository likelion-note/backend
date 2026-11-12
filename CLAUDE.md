# CLAUDE.md

이 파일은 Claude Code가 매 세션 시작 때 읽는 프로젝트 안내서예요. 결정이 바뀌면 같이 고쳐 주세요.

**응답은 항상 한국어로.**

## 프로젝트

**멋사수첩**: 멋쟁이사자처럼 동아리용 앱. 지난 기수가 프로젝트에서 막혔던 순간과 넘긴 방법(성장일지)을 익명 노트로 모아, 지금 기수가 자기 파트·주차에 맞춰 보게 하는 서비스. 멋사에서 가볍게 하는 마지막 프로젝트라 과하게 설계하지 않기.

- 계정 유형: 아기사자(MEMBER) · 운영진(ADMIN)
- 로그인: 구글 로그인 + 처음 한 번 초대코드(기수·계정 유형 결정)
- 핵심 흐름: 성장일지 작성 → AI 정리(이름 가리기·태그·제목) → 운영진이 이름 없이 검토 → 승인되면 작성자가 고른 범위로 노트 게시판에 공개
- 공개 범위: `NOW`(지금 같은 기수에게, 같은 프로젝트 팀원 제외) / `NEXT`(다음 기수부터)
- 기타: 회의 도우미(회의 정리·공유), 파트별 오늘 할 일, 포트폴리오 요약, 운영진의 멤버 배정·일정·초대코드 관리
- **시연 제외**: 추천 노트, 승인된 노트 공개 취소·삭제, 도움돼요 알림

## 문서 (docs/)

- `docs/feature_spec.md`: 기능·화면 명세 v2.0. 규칙이 애매하면 여기 먼저 확인
- `docs/erd.md`: **확정 ERD** (2026-11-11). 엔티티는 이 문서가 기준. 엔티티 바꾸면 같이 고치기
- `docs/api_spec.md`: API 명세. B 파트(일지·노트·검토·포트폴리오)는 여기, A 파트는 언니 노션 명세. 원본은 노션이라 바뀌면 둘 다 고치기
- `docs/decisions.md`: 명세에서 바뀐 것, 회의에서 정한 것
- `docs/code_map.md`: A 담당 + 공통 코드의 파일별 역할과 로직
- `src/main/resources/seed/`: 가이드·회의 도우미 데이터와 시연용 예시 데이터. 설명은 `seed/README.md`
- 세부 규칙이 문서에도 없으면 추측하지 말고 물어보기

## 기술 스택

- Spring Boot 3.4 · Java 17+ · Gradle · Spring Data JPA · Lombok
- DB: 로컬 H2 메모리(MySQL 모드) / 배포 MySQL
- 프론트는 별도 레포(Vercel 예정). CORS 허용 주소는 `CORS_ORIGINS` 환경변수
- 서버: **AWS EC2** (A 담당). 설정은 계속 환경변수로만 받기

## 실행

```bash
./gradlew bootRun     # http://localhost:8080/api/health
./gradlew test        # 엔티티 매핑 확인 (NotebookApplicationTests)
```
배포: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ORIGINS`. 비밀값은 yml에 쓰지 않기.

## 현재 상태 (2026-11-12)

- A 완료: 구글 로그인 + 초대코드 가입 + JWT(`Authorization: Bearer`), local 전용 `X-User-Id`(`app.auth.dev-mode`), `/api/me`, 배정(`AssignmentService`). 자세한 건 `docs/code_map.md`
- `ErrorCode` 공통 5개로 정리 완료 (A). 문구는 `new BusinessException(code, message)`로 전달
- B(journal·note)는 아직 `entity`, `repository`만 있음. `controller / service / dto` 없음
- `./gradlew test` 통과. `gradlew` 래퍼 있음
- **로컬 JDK는 17 사용.** PC에 JDK 27도 설치돼 있지만 Gradle 래퍼(9.3.0)는 JDK 17로만 확인함. 빌드가 이상하면 `JAVA_HOME`이 jdk-17을 가리키는지 먼저 확인
- **엔티티가 아직 확정 ERD와 다 맞지는 않음** → 아래 "ERD 맞추기" 참고
- 시드 JSON은 있지만 읽는 코드(로더)는 없음 (초대코드만 `LocalInviteCodeSeeder`로 local에 넣음)

## ERD 맞추기 (할 일)

아래 ERD 맞추기, API 공통 규칙, 성장일지·익명성 규칙은 A·B가 합의해 확정한 내용이에요 (2026-11-12).

테이블 10개: `users`, `project_category`, `project_member`, `invite_code`, `events`, `guide_check`, `meeting`, `journal`, `note_reaction`, `portfolio_summary`

**B (journal·note)**
- [ ] `Timing`: `W2PLUS`, `W1_2`, `DURING`, `AFTER`. `Tag`는 지금 코드 값 그대로
- [ ] `Journal`: `reviewer`(User, nullable), `reviewedAt` 추가. `tags`는 JSON 배열. `category`, `submittedAt` 삭제. `readCount`·`helpCount`도 삭제 (ERD: 개수는 매번 셈)
- [ ] `NoteRead` + `NoteHelp` → `NoteReaction` 하나로 (행 있으면 읽음, `helped`면 도움돼요, (user, journal) 유니크)
- [ ] `PortfolioSummary` 엔티티 새로 (하루 5회 제한 계산용)
- [ ] `JournalRepository` 정렬 `submittedAt` → `updatedAt`

**A (user·auth·guide·meeting)**
- [x] `Part`: `OPS` 추가
- [x] `User`: `currentProject` 삭제. 지금 팀은 배정의 `is_current`로 찾기
- [x] 배정에 `part`, `isCurrent` 추가. 재배정 시 행을 지우지 않고 `false`로 → 단, 코드는 아직 `ProjectAssignment`(ERD는 `project_member`). 운영 분류 파트도 코드는 `OPS`, ERD는 NULL
- [ ] `ProjectCategory`: `anonLabel` (코드는 `teamLetter`라는 이름으로 있음)
- [ ] `InviteCode`: `expiresAt` NULL 허용 (NULL = 만료 없음)
- [ ] `MeetingType`: `FIRST`, `IDEA`, `MIDCHECK`, `RETRO` / `EventType`: `KICKOFF`, `MAJOR`, `REGULAR` / `ProjectKind`: `TEAM`, `OPERATION`

## 패키지와 담당 (백엔드 2명)

```
com.likelion.notebook
├─ global/   공통 응답·예외·설정·@CurrentUser   ← 같이
├─ auth/     구글 로그인, 초대코드              ← A
├─ user/     멤버, 프로젝트 분류, 배정 이력      ← A
├─ guide/    일정, 주차·D-day, 오늘 할 일        ← A
├─ meeting/  회의 정리, 공유 원문                ← A
├─ journal/  성장일지, AI 정리, 검토, 포트폴리오  ← B
└─ note/     노트 게시판, 검색, 읽음·도움돼요     ← B
```
A(언니)는 배포(AWS EC2)·CI/CD도 담당. 기능 폴더 안은 `controller / service / repository / entity / dto`.

## API 공통 규칙

- 성공 `{ "success": true, "data": ... }` / 실패 `{ "success": false, "code", "message" }`. 코드에서는 `ApiResponse.ok(data)`
- 에러 코드는 **5개만**: `INVALID_INPUT`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `INTERNAL_ERROR`(500). 상황은 message로 구분. 던질 때는 `throw new BusinessException(ErrorCode.XXX)`
- 값은 API·DB 모두 대문자, JSON 키는 camelCase
- 날짜 `YYYY-MM-DD` / 시간까지는 `YYYY-MM-DDTHH:mm:ss` (KST)
- 페이징은 노트 목록만: `page` 0부터, `size` 기본 20, 최대 50
- 인증: `Authorization: Bearer {accessToken}` (JWT, 구현 완료). **local 프로필에서만** `X-User-Id` 헤더도 받음 (`app.auth.dev-mode=true`). 배포 서버에서는 반드시 꺼져 있어야 함

## 코드 규칙

- 로그인 사용자는 컨트롤러에서 `@CurrentUser Long userId`. **이게 붙은 API만 로그인 필요**하니 빠뜨리지 않기. 운영진 여부는 서비스에서 따로 확인
- 엔티티에 setter 금지. 상태 변경은 엔티티 메서드로 (`journal.submit()`, `approve()`, `reject(reason)`)
- 엔티티를 그대로 응답하지 않기. 항상 DTO
- 브랜치: `main` ← `develop` ← `feat/기능명`

## 성장일지 규칙 (B)

- AI 정리 전 필수: `activity`·`timing`·`scope`, 막힌 것 30자 이상, 넘긴 방법 필수
- 자동 저장: 입력 멈추고 3초 뒤. 첫 입력 때 `POST`로 id를 받고 이후 `PUT`
- 보완 질문: DB에 저장 안 함. 처음 정리할 때(`summary_title`이 비어 있을 때)만 생성하고, 답은 `raw_solved` 끝에 붙임
- 태그는 검토 요청 때 **빼기만** 가능 (추가·변경 불가)
- 노트 검색 대상은 제목·본문·태그. `authorLabel`(팀 가명)은 검색 안 함
- 명세의 "분류가 회의인 노트"는 태그 `MEETING_RUN`으로 대신 (`GET /api/notes?tag=MEETING_RUN&size=2`)

## 익명성 규칙 (가장 중요)

- 작성자 이름은 **운영진 멤버 목록**에서만 보임. 노트·검토 화면·알림 응답 DTO에 `author`, 이름, `projectId`를 넣지 않기. 작성자 표시는 `authorLabel` 문자열 하나 (만드는 법은 `docs/api_spec.md`)
- 노트 공개 여부(같은 팀원 숨김 포함)는 **서버 쿼리에서** 거르기. 프론트 필터링 금지
- 팀 가명("B팀", `anon_label`)은 **지난 기수 노트에만** 표시. 같은 기수 노트는 검색어로도 팀이 걸리면 안 됨 (데모에 이 버그가 있었음)
- 검토 화면 작성자 표시는 공개 범위와 상관없이 **팀 빼고 "14기 · PM"만** (확정)
- 같은 팀원 숨김: 그 일지 프로젝트의 `project_member`(`is_current=false` 포함)에게만 숨김. 미분류 일지는 안 숨김. 작성자 본인은 항상 봄

## 명세 ↔ 데모 차이

정해진 것: 일지 필수(넘긴 방법도 필수), 자동 저장(입력 멈추고 3초 뒤), 초대코드 만료(`expiresAt` NULL = 만료 없음, `isUsable()`에서 체크). 추천 점수는 추천 노트가 시연 제외라 보류.

| 항목 | 명세 | 데모 |
|---|---|---|
| 회의 불러오기 | 지난 7일 | 문구는 "이번 주" |

아직 정해지지 않은 것: 운영진이 1명일 때 검토, 미분류 일지 포트폴리오.

## 다음 할 일

1. ~~`./gradlew test` 통과시키기~~ 완료
2. ~~A: 구글 로그인 + 초대코드 → JWT, local 전용 `X-User-Id`~~ 완료
3. 엔티티를 확정 ERD에 맞추기 (위 "ERD 맞추기", 각자 영역)
4. B: `docs/api_spec.md` 대로 일지 작성·자동 저장 API부터 (`X-User-Id`로 local 테스트 가능)
5. A 파트 API 명세를 `docs/api_spec.md`로 옮기기 (프론트와 공유)
6. 시드 로더: `guide.json`, `meeting-helper.json`은 시작 시 메모리로, `demo-data.json`은 `local` 프로필에서만 DB로
