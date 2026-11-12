# CLAUDE.md

이 파일은 Claude Code가 매 세션 시작 때 읽는 프로젝트 안내서예요. 결정이 바뀌면 같이 고쳐 주세요.

**응답은 항상 한국어로.**

## 프로젝트

**멋사수첩**: 멋쟁이사자처럼 동아리용 앱. 지난 기수가 프로젝트에서 막혔던 순간과 넘긴 방법(성장일지)을 익명 노트로 모아, 지금 기수가 자기 파트·주차에 맞춰 보게 하는 서비스. 멋사에서 가볍게 하는 마지막 프로젝트라 과하게 설계하지 않기.

- 계정 유형: 아기사자(MEMBER) · 운영진(ADMIN)
- 로그인: 구글 로그인 + 처음 한 번 초대코드(기수·계정 유형 결정)
- 핵심 흐름: 성장일지 작성 → AI 정리(이름 가리기·태그·제목) → 운영진이 이름 없이 검토 → 승인되면 작성자가 고른 범위로 노트 게시판에 공개
- 공개 범위: `NOW`(지금 같은 기수에게, 같은 프로젝트 팀원 제외) / `NEXT`(다음 기수부터)
- 기타: 회의 도우미(회의 정리·공유), 파트별 오늘 할 일, 추천 노트, 포트폴리오 요약, 운영진의 멤버 배정·일정·초대코드 관리

## 문서 (docs/)

- `docs/feature_spec.md`: 기능·화면 명세 v2.0. 규칙이 애매하면 여기 먼저 확인
- `docs/api_spec.md`: API 목록 (작성 중)
- `docs/erd.md`: ERD (작성 중). 엔티티 바꾸면 같이 고치기
- `docs/decisions.md`: 명세에서 바뀐 것, 회의에서 정한 것
- `src/main/resources/seed/`: 가이드·회의 도우미 데이터와 시연용 예시 데이터. 설명은 `seed/README.md`
- 세부 규칙이 문서에도 없으면 추측하지 말고 물어보기

## 기술 스택

- Spring Boot 3.4 · Java 17+ · Gradle · Spring Data JPA · Lombok
- DB: 로컬 H2 메모리(MySQL 모드) / 배포 MySQL
- 프론트는 별도 레포(Vercel 예정). CORS 허용 주소는 `CORS_ORIGINS` 환경변수
- 서버(배포처)는 **아직 미정**: AWS 신규 계정 크레딧(EC2 t4g/t3.small + Docker MySQL 또는 RDS) vs Render(Docker) + Supabase. 코드는 환경변수로만 설정을 받게 해서 어느 쪽이든 안 바뀌게 유지

## 실행

```bash
./gradlew bootRun     # http://localhost:8080/api/health
./gradlew test        # 엔티티 매핑 확인 (NotebookApplicationTests)
```
배포: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ORIGINS`. 비밀값은 yml에 쓰지 않기.

## 현재 상태 (2026-11-09)

- 뼈대만 있음: `global` 공통 코드 + 각 기능의 `entity`, `repository`
- `./gradlew test` 통과 (엔티티 매핑 정상). `gradlew` 래퍼 있음
- **로컬 JDK는 17 사용.** PC에 JDK 27도 설치돼 있지만 Gradle 래퍼(9.3.0)는 JDK 17로만 확인함. 빌드가 이상하면 `JAVA_HOME`이 jdk-17을 가리키는지 먼저 확인
- 시드 JSON은 있지만 읽는 코드(로더)는 없음
- `controller / service / dto`는 아직 없음

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
A는 배포·CI/CD도 담당. 추천 노트는 여유 있는 사람이. 기능 폴더 안은 `controller / service / repository / entity / dto`.

## 코드 규칙

- 모든 API 응답은 `ApiResponse`로 감싸기 (`ApiResponse.ok(data)`)
- 에러 코드는 **5개만**: `INVALID_INPUT`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `INTERNAL_ERROR`(500). 새 코드 추가 금지
- 상황별 문구는 메시지로 구분: `throw new BusinessException(ErrorCode.INVALID_INPUT, "막힌 것을 30자 이상 적어 주세요")`. 메시지 생략 시 기본 문구
- 로그인 사용자는 컨트롤러에서 `@CurrentUser Long userId`. 지금은 임시로 `X-User-Id` 헤더 값 사용. JWT 붙이면 `CurrentUserArgumentResolver`만 교체 (**배포 전 필수**)
- 엔티티에 setter 금지. 상태 변경은 엔티티 메서드로 (`journal.submit()`, `approve()`, `reject(reason)`)
- 엔티티를 그대로 응답하지 않기. 항상 DTO
- 브랜치: `main` ← `develop` ← `feat/기능명`

## 익명성 규칙 (가장 중요)

- 작성자 이름은 **운영진 멤버 목록**에서만 보임. 노트·검토 화면·알림 응답 DTO에 `author`, 이름, `projectId`를 넣지 않기
- 노트 공개 여부(같은 팀원 숨김 포함)는 **서버 쿼리에서** 거르기. 프론트 필터링 금지
- 팀 글자("A팀")는 **지난 기수 노트에만** 표시. 같은 기수 노트는 검색어로도 팀이 걸리면 안 됨 (데모에 이 버그가 있었음)
- 검토 화면에서는 공개 범위와 상관없이 **팀을 빼고 "기수 · 파트"만** 보여주기로 제안된 상태 (팀+파트면 운영진이 작성자를 특정할 수 있어서). 팀 확정 전
- 같은 팀원 숨김은 `ProjectAssignment`(배정 이력) 기준. 지난 프로젝트 팀원에게도 숨길지는 미정

## 명세 ↔ 데모 차이 (팀이 하나로 정해야 함)

| 항목 | 명세 | 데모 |
|---|---|---|
| 추천 점수 | 파트 +3, 행사·구간 +3, 태그 +1(최대 2) | 행사만 같으면 +1, "행사 후" +3 규칙 추가 |
| 일지 필수 | 막힌 것 30자 | 넘긴 방법도 필수 |
| 회의 불러오기 | 지난 7일 | 문구는 "이번 주" |
| 초대코드 만료 | 있음 | 체크 안 함 → 엔티티 `isUsable()`에서 만료까지 체크하도록 반영함 |
| 자동 저장 | 3초마다 | 입력 멈추고 3초 뒤 (이쪽 추천) |

명세에서 빠진 것: 도움돼요 알림 기능 여부, 승인된 일지 공개 취소·삭제, 운영진이 1명일 때 검토, 미분류 일지 포트폴리오.

## 다음 할 일

1. ~~`./gradlew test` 통과시키기~~ 완료
2. A: 구글 로그인(OAuth2) + 초대코드 → JWT 발급. B: 일지 작성·자동 저장 API (`X-User-Id`로 테스트)
3. 둘이 API 명세 확정 (`docs/api_spec.md`, 프론트와 공유)
4. 시드 로더: `guide.json`, `meeting-helper.json`은 시작 시 메모리로, `demo-data.json`은 `local` 프로필에서만 DB로