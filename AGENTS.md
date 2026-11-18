# AGENTS.md

잠깐의 쉼(pulse) 백엔드(Spring Boot)에서 일하는 코딩 에이전트의 작업 규칙이다. 프로젝트 설명은 [README.md](README.md)에 있다.

## 1. 시작할 때 읽는 순서

1. `README.md`의 "API 구현 현황"과 "기여 기록": 무엇이 끝났고 누가 마지막으로 무엇을 했는지 확인한다.
2. `openspec list`, 그다음 이어받을 변경의 `openspec/changes/<이름>/proposal.md`와 `tasks.md`: 체크되지 않은 작업이 할 일이다.
3. `docs/API명세서.md`의 해당 절: 요청·응답 필드와 오류 코드는 명세가 기준이다.

사람이 할 일을 정해 주지 않았으면 위 순서로 상태를 파악하고, 이어받을 변경과 작업을 먼저 제안한다.

## 2. 아키텍처 규칙

- 패키지는 `com.ganzithon.pulse` 아래 `controller` · `service` · `repository` · `entity` · `dto/<도메인>` · `global/{auth,config,exception,response,util}`이다.
- 의존 방향은 Controller → Service → Repository다. Controller는 Entity와 Repository를 직접 다루지 않는다. 비즈니스 로직은 Service에 둔다.
- DTO는 `dto/<도메인>/`에 record 하나당 파일 하나로 둔다. Entity → DTO 변환은 DTO의 정적 메서드 `from()`으로 한다.
- 로그인한 사용자 ID는 `@RequestAttribute(AuthInterceptor.USER_ID) Long userId`로 받는다.
- 오류는 `throw new BusinessException(ErrorCode.X)`로 던진다. 새 오류 코드는 명세 2.4에 먼저 적는다.
- 성공 응답은 `ApiResponse.ok(data)` 또는 `ApiResponse.message("처리되었습니다.")`로 돌려준다.
- 응답 시각은 `TimeFormat.iso(instant)`로 만든다(`2026-11-15T14:20:00+09:00`). Entity에는 `Instant`로 저장한다.
- 기존 코드처럼 생성자 주입을 쓰고, 설정값은 `application.yml`의 `pulse.*`에 두고 `@Value`로 받는다.

## 3. 하지 않는 것

- Spring Security 필터 체인(`spring-boot-starter-security`) 도입. 인증은 `AuthInterceptor`가 맡는다.
- Lombok 사용.
- 근거 없는 새 의존성 추가. 필요하면 이유를 적어 사람에게 먼저 묻는다.
- 명세에 없는 API를 OpenSpec 제안 없이 추가.
- `docs/API명세서.md`와 다르게 구현. 달라져야 하면 6장 순서대로 명세를 먼저 고친다.

## 4. 검증

- 완료 조건은 `./gradlew test` 통과다.
- 테스트는 `PulseFlowTest`처럼 `@SpringBootTest` + MockMvc로 API를 직접 호출한다.
- 모든 테스트가 Spring 컨텍스트와 H2 DB 하나를 공유한다. 테스트마다 다른 `loginId`를 써서 실행 순서에 의존하지 않게 한다.
- 새 API는 성공 응답과 명세의 오류 코드를 각각 하나 이상 테스트한다.

## 5. spec-driven 워크플로

| 단계 | 스킬 (Claude Code) | CLI |
|---|---|---|
| 제안 | `/openspec-propose` | `openspec new change <이름>` |
| 검토 | | `openspec validate <이름> --strict` |
| 구현 | `/openspec-apply-change <이름>` | `openspec status --change <이름>` |
| 아카이브 | `/openspec-archive-change <이름>` | `openspec archive <이름>` |

- 전체 상태는 `openspec list`, `openspec show <이름>`, `openspec validate --all --strict`로 본다.
- 구현은 `tasks.md`의 체크박스를 하나씩 끝내며 진행한다. 제안을 고쳐야 하면 `/openspec-update-change`를 쓴다.
- 스킬 원본은 `.agents/skills/openspec-*`이고, `.claude/skills/`는 그 링크다.
- OpenSpec 산출물은 한국어로 쓴다(`openspec/config.yaml`).

## 6. 작업을 마칠 때

1. README "API 구현 현황" 표의 상태와 OpenSpec 열을 고친다.
2. 구현이 `docs/API명세서.md`와 달라졌으면 명세를 먼저 고친다. 그리고 FE 팀에 알려야 한다고 사람에게 보고한다.
3. 커밋한 뒤 `./scripts/update-contributions.sh`를 실행한다. `CONTRIBUTIONS` 마커 사이는 손으로 고치지 않는다.
4. README "커밋 대기 중인 작업"에 적힌 항목이 커밋됐으면 그 항목을 지운다.

## 7. 커밋 규칙

- 커밋과 푸시는 사람이 요청할 때만 한다.
- 제목은 `feat:` · `fix:` · `refactor:` · `docs:` · `test:` 뒤에 한국어로 쓴다.
- 기존 커밋 예: `feat: 인증·심박 알림·활동 기록 API 초기 구현`, `docs: API 명세서 v0.2 (팀 결정 반영)`.
