# 멋사수첩 백엔드

지난 기수가 프로젝트에서 막혔던 순간과 넘긴 방법을 익명 노트로 모아, 지금 기수가 자기 파트·주차에 맞춰 볼 수 있게 하는 서비스의 백엔드예요.

## 기술 스택

| 구분 | 사용 |
|---|---|
| 언어 / 프레임워크 | Java 17+, Spring Boot 3.4 |
| 빌드 | Gradle |
| DB | 로컬 H2 (메모리) / 배포 MySQL |
| ORM | Spring Data JPA |
| 기타 | Lombok, Bean Validation |

## 시작하기

### 처음 한 번

1. JDK 17 이상 설치 (IntelliJ에서 프로젝트 열 때 다운로드 가능)
2. IntelliJ에서 `backend` 폴더 열기 → Gradle 프로젝트로 자동 인식
3. `gradlew`가 없으면 터미널에서 `gradle wrapper` 실행 후, 생긴 `gradlew`, `gradlew.bat`, `gradle/` 폴더까지 커밋

### 실행

```bash
./gradlew bootRun
```

- 서버 확인: http://localhost:8080/api/health
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:notebook`, 사용자 `sa`, 비밀번호 없음)

로컬은 H2 메모리 DB라 MySQL 없이 바로 돌아가요. 서버를 끄면 데이터는 사라져요.

### 테스트

```bash
./gradlew test
```

## 환경변수 (배포)

| 이름 | 설명 | 예시 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 실행 환경 | `prod` |
| `DB_URL` | MySQL 주소 | `jdbc:mysql://호스트:3306/notebook` |
| `DB_USERNAME` | DB 사용자 | |
| `DB_PASSWORD` | DB 비밀번호 | |
| `CORS_ORIGINS` | 허용할 프론트 주소 (쉼표로 여러 개) | `https://notebook.vercel.app` |

**비밀번호나 키를 `application.yml`에 적어서 커밋하지 않기.** `.env`는 `.gitignore`에 들어 있어요.

## 폴더 구조

```
backend/
├─ CLAUDE.md          Claude Code용 프로젝트 안내
├─ docs/              명세 문서
│  ├─ feature-spec.md   기능명세서
│  ├─ api-spec.md       API 명세서
│  ├─ erd.md            ERD
│  └─ decisions.md      결정사항
└─ src/main/java/com/likelion/notebook/
   ├─ global/   공통: 응답 형식, 예외, 설정, @CurrentUser
   ├─ auth/     구글 로그인, 초대코드
   ├─ user/     멤버, 프로젝트 분류, 배정
   ├─ guide/    일정, 주차·D-day, 오늘 할 일
   ├─ meeting/  회의 정리, 공유
   ├─ journal/  성장일지, AI 정리, 검토, 포트폴리오
   └─ note/     노트 게시판, 검색, 읽음·도움돼요
```

각 기능 폴더 안은 이렇게 나눠요.

```
journal/
├─ controller/
├─ service/
├─ repository/
├─ entity/
└─ dto/
```

## 역할 분담

| | A | B |
|---|---|---|
| 담당 패키지 | `auth`, `user`, `guide`, `meeting` | `journal`, `note` |
| 먼저 | 구글 로그인 + 초대코드 → JWT | 성장일지 작성·자동 저장 |
| 그다음 | 멤버 배정, 일정, 주차 계산, 회의 정리 | 운영진 검토, 노트 게시판·검색 |
| 마지막 | 배포, CI/CD | AI 정리 연동, 포트폴리오 |

`global`은 같이 관리하고, 추천 노트는 진행 상황 보고 여유 있는 사람이 맡아요.

## 개발 규칙

### API 응답

모든 API는 `ApiResponse`로 감싸서 응답해요.

```json
{ "success": true, "data": { } }
```
```json
{ "success": false, "code": "JOURNAL_NOT_FOUND", "message": "일지를 찾을 수 없어요" }
```

### 에러 처리

서비스에서는 `BusinessException`만 던져요.

```java
throw new BusinessException(ErrorCode.JOURNAL_NOT_FOUND);
```

새 에러는 `ErrorCode`의 자기 담당 영역에 추가해요.

### 로그인 사용자

컨트롤러에서 `@CurrentUser Long userId`로 받아요.

```java
@PostMapping("/api/journals")
public ApiResponse<JournalResponse> create(@CurrentUser Long userId, @RequestBody @Valid JournalRequest req) { ... }
```

로그인 기능이 붙기 전까지는 요청 헤더 `X-User-Id: 1` 값을 그대로 써요. 로그인이 붙으면 `CurrentUserArgumentResolver`만 바뀌고 컨트롤러 코드는 그대로예요. **배포 전 반드시 교체.**

### 엔티티

- setter 쓰지 않기. 상태 변경은 엔티티 메서드로 (`journal.submit()`, `journal.approve()`, `journal.reject(reason)`)
- 엔티티를 그대로 응답하지 않고 항상 DTO로 변환

### 익명성

이 서비스에서 가장 중요한 규칙이에요.

- 작성자 이름은 운영진의 멤버 목록에서만 보여요. 노트·검토·알림 응답에는 작성자, 이름, 프로젝트 id를 넣지 않아요.
- 노트 공개 여부(같은 팀원에게 숨기기 포함)는 **서버에서** 걸러요. 프론트에서 거르면 개발자도구로 다 보여요.
- 팀 이름("A팀")은 지난 기수 노트에만 보여요. 같은 기수 노트는 검색으로도 팀이 드러나면 안 돼요.

## 브랜치 / 커밋

```
main ← develop ← feat/기능명
```

- 기능 브랜치 예: `feat/journal-submit`, `fix/invite-code-expire`
- `develop`으로 PR 올리고 서로 한 번씩 리뷰한 뒤 합치기
- 커밋 메시지: `feat: 성장일지 검토 요청 API`, `fix: 초대코드 만료 체크`, `docs: ERD 추가`, `refactor: ...`

## 문서

- 기능명세서: `docs/feature-spec.md`
- API 명세서: `docs/api-spec.md`
- ERD: `docs/erd.md`
- 결정사항: `docs/decisions.md`

명세가 바뀌면 문서도 같이 고쳐 주세요.