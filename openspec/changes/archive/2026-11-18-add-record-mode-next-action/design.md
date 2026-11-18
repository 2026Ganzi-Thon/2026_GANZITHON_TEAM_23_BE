# Design

## Context

변경 이유는 proposal.md의 Why를 본다. 구현 전에 알아야 할 현재 상태는 다음과 같다.

- `dto/record/RecordRequest`에는 `pictureId`, `alertId`, `beforeScore`, `afterScore`만 있다. 서버는 모르는 JSON 필드를 오류 없이 무시한다. 그래서 프론트가 지금 보내는 `mode`는 조용히 버려진다(2026-11-18 로컬 실행으로 확인).
- `entity/ActivityRecord`에는 모드와 다음 행동 열이 없다. `service/RecordService.DEFAULT_ACTIVITY`("숨은그림찾기")의 주석은 "모드가 확정되면 요청값으로 받도록 바꿀 자리"라고 되어 있다. 팀 결정(명세 8.1 1번)으로 활동 이름은 고정이고 모드는 별도 필드가 되었으므로 이 주석은 더 이상 맞지 않는다.
- `repository/RecordRepository`에는 사용자별 목록 조회(`findByUserIdOrderByCreatedAtDescIdDesc`)만 있다.
- `global/exception/ErrorCode`에 `RECORD_001`이 없다.
- 잘못된 enum 문자열처럼 JSON 본문을 읽지 못하는 경우, 숫자가 아닌 경로 변수처럼 타입이 맞지 않는 경우는 `GlobalExceptionHandler`가 이미 400 `COMMON-001`로 바꿔 준다. 이번 변경에서 예외 처리기를 고칠 필요는 없다.

## Goals / Non-Goals

**Goals:**

- 명세 6.1~6.3 계약과 같은 요청·응답을 만든다. 필드 이름, 값, 상태 코드, 오류 코드가 모두 같아야 한다.
- `Mode` enum을 `add-picture-next`(5.1)와 함께 쓸 수 있는 위치에 둔다.

**Non-Goals:**

- 다음 행동의 변경 이력은 남기지 않는다. 명세는 마지막 값만 요구한다.
- 기존 기록 데이터의 모드를 채우는 작업은 하지 않는다. 로컬 DB는 재시작하면 비워진다.

## Decisions

### 1. 모드와 다음 행동은 `entity/`의 enum으로 두고 문자열로 저장한다

`entity/Mode`(`COLOR`, `OBJECT`)와 `entity/NextAction`(`IMPROVED`, `RETRY`, `HELP`)을 만든다. `ActivityRecord`에는 `@Enumerated(EnumType.STRING)`으로 매핑한다. JSON 값과 DB 값이 enum 이름과 같아서 H2 콘솔에서 읽기 쉽다.

- 검토한 대안: `EnumType.ORDINAL`. enum 순서를 바꾸면 저장된 값의 뜻이 바뀌므로 고르지 않았다.
- 검토한 대안: `String` 열에 두고 서비스에서 값을 검사. 같은 검사가 여러 곳에 생기고 오타를 컴파일러가 잡지 못해 고르지 않았다.
- `Mode`는 숨은그림 API(`add-picture-next`)도 쓴다. 두 변경 중 먼저 구현하는 쪽이 `entity/Mode`를 만들고, 나중 쪽은 그것을 쓴다.

### 2. 다음 행동은 기록 테이블의 열 하나로 둔다

`ActivityRecord`에 `nextAction`(NULL 허용) 열을 추가하고 `recordNextAction(NextAction)` 메서드로 바꾼다. 다시 보내면 같은 열을 덮어쓴다.

- 검토한 대안: 다음 행동마다 행을 쌓는 별도 테이블. 이력을 남길 수 있지만 명세는 마지막 값만 요구하고 목록 응답도 값 하나다. 조회할 때 조인이 늘어서 고르지 않았다.

### 3. 기록 소유 확인은 `findByIdAndUserId` 한 번으로 한다

`RecordRepository`에 `Optional<ActivityRecord> findByIdAndUserId(Long id, Long userId)`를 추가한다. 결과가 없으면 `BusinessException(ErrorCode.RECORD_001)`을 던진다. 기록이 없는 경우와 다른 사용자의 기록인 경우를 같은 404로 처리해 다른 사용자의 기록이 있는지 드러내지 않는다. 기존 알림 거절(`AlertRepository.findByAlertIdAndUserId`)과 같은 방식이다.

- 검토한 대안: `findById`로 찾은 뒤 주인을 비교해 다른 사용자면 403. 명세 6.3이 두 경우 모두 `RECORD-001`(404)이라고 정했으므로 고르지 않았다.

### 4. 입력 검증은 DTO 애너테이션과 기존 예외 처리기에 맡긴다

- `RecordRequest.mode`: `@NotNull(message = "모드가 필요해요.")`
- `NextActionRequest.action`: `@NotNull(message = "다음 행동이 필요해요.")`
- 정의되지 않은 enum 문자열은 JSON을 읽는 단계에서 실패하므로 기존 처리기가 400 `COMMON-001`과 빈 `errors`를 돌려준다.
- 숫자가 아닌 `recordId`는 기존 타입 불일치 처리기가 400 `COMMON-001`과 `errors[0].field = "recordId"`를 돌려준다.
- 검토한 대안: enum 필드를 `String`으로 받고 `@Pattern`으로 검사해 `errors`에 이유를 담기. 명세는 이 경우 프론트가 "개발 오류, 무시"로 처리한다고 했으므로 코드를 늘릴 이유가 없어 고르지 않았다.

### 5. 응답 필드 순서는 명세 6.1 표를 따른다

`RecordResponse`의 record 구성 요소 순서를 `recordId, activityName, mode, pictureId, beforeScore, afterScore, nextAction, createdAt`으로 둔다. JSON 필드 순서는 계약이 아니지만 명세 예시와 같으면 사람이 비교하기 쉽다. `nextAction`이 `null`이어도 키는 응답에 들어간다(`api-common`의 빈 값 표기 규칙).

### 6. `pictureId`와 `alertId`는 지금처럼 확인 없이 저장한다

현재 동작(`activity-record`의 "그림 ID와 알림 ID의 저장")을 바꾸지 않는다. 명세 6.2는 저장에 실패해도 프론트가 다음 화면으로 넘어간다고 정했다. 서버가 존재하지 않는 그림이나 알림을 이유로 거절하면 기록만 사라지고 사용자 흐름은 그대로이므로 막지 않는다.

## 추가·변경 클래스

| 구분 | 경로 (`com.ganzithon.pulse` 아래) | 내용 |
|---|---|---|
| 추가 | `entity/Mode` | enum `COLOR`, `OBJECT`. `add-picture-next`와 함께 쓴다 |
| 추가 | `entity/NextAction` | enum `IMPROVED`, `RETRY`, `HELP` |
| 변경 | `entity/ActivityRecord` | `mode`(`@Enumerated(STRING)`, `@Column(nullable = false)`), `nextAction`(`@Enumerated(STRING)`, NULL 허용) 필드. 생성자에 `mode` 인자 추가. `recordNextAction(NextAction)`, `getMode()`, `getNextAction()` |
| 변경 | `dto/record/RecordRequest` | 맨 앞에 `@NotNull(message = "모드가 필요해요.") Mode mode` 추가 |
| 변경 | `dto/record/RecordResponse` | `mode`, `nextAction` 추가, 순서는 결정 5 |
| 추가 | `dto/record/NextActionRequest` | `record NextActionRequest(@NotNull(message = "다음 행동이 필요해요.") NextAction action)` |
| 변경 | `repository/RecordRepository` | `findByIdAndUserId(Long id, Long userId)` 추가 |
| 변경 | `service/RecordService` | `save`에서 `mode` 저장, `recordNextAction(Long userId, Long recordId, NextActionRequest)` 추가(`@Transactional`). `DEFAULT_ACTIVITY` 주석을 "팀 결정으로 활동 이름 고정, 모드는 `mode` 필드"로 고친다 |
| 변경 | `controller/RecordController` | `@PostMapping("/{recordId}/next-action")`. `@PathVariable Long recordId`, `@Valid @RequestBody NextActionRequest`. `ApiResponse.message("처리되었습니다.")`, 200 |
| 변경 | `global/exception/ErrorCode` | `RECORD_001("RECORD-001", HttpStatus.NOT_FOUND, "기록을 찾을 수 없어요.")` |
| 변경 | `src/test/java/com/ganzithon/pulse/PulseFlowTest` | 기존 기록 저장 본문에 `mode` 추가, 새 시나리오 테스트 추가 |

## Risks / Trade-offs

- [운영 DB에 이미 기록이 있으면 NOT NULL `mode` 열 추가가 실패하거나 기존 행에 의도하지 않은 값이 들어갈 수 있다. 운영 프로필은 `ddl-auto: update`다.] → `docs/개발현황.md` 3.2 기준 운영 DB는 아직 연결 전이라 지금은 영향이 없다. 배포 후에 이 변경을 적용한다면 열을 NULL 허용으로 먼저 추가하고 값을 채운 뒤 NOT NULL로 바꾸는 SQL을 따로 실행한다.
- [**BREAKING**: `mode` 없이 저장하던 클라이언트는 400을 받는다. 프론트는 저장 실패를 무시하고 다음 화면으로 넘어가므로 기록이 조용히 사라질 수 있다.] → v0.2를 반영한 프론트 `src/api`는 `mode`를 보낸다. 통합 테스트로 `mode` 누락이 400이 되는 것을 고정하고, 배포 전에 프론트와 한 번 맞춰 본다.
- [6.2 저장이 실패하면 `recordId`가 없어 6.3을 부를 수 없다.] → 명세대로 프론트 흐름은 막지 않는다. 그 활동의 다음 행동은 기록되지 않는다.
- [Figma 10번의 "나아졌어요: 기록하고 마쳐요" 문구는 기록 저장이 이 버튼에서 일어나는 것처럼 읽힌다(`docs/화면흐름.md` 6장 "Figma와 명세 v0.2의 차이"의 기록 저장 시점 항목).] → 서버는 6.2 저장 뒤에 6.3을 받는 순서만 지원한다. 프론트가 6.2를 먼저 부르는지 확인한다.

## Migration Plan

- 로컬과 테스트: H2 메모리 DB에 `create-drop`이라 할 일이 없다.
- 운영: 위 Risks의 첫 항목을 따른다.
- 되돌리기: 코드를 되돌리면 된다. 로컬 DB는 재시작하면 새 스키마로 만들어진다.

## Open Questions

없음. 아래 두 건은 구현 때 이 변경에서 함께 해결했다.

- 소수·문자열 점수: 전역 Jackson 설정(`accept-float-as-int: false`, `allow-coercion-of-scalars: false`)을 먼저 확인했다. 설정은 효과가 있지만 `/heart-rate/spikes`의 `bpm`(명세 4.1에 "number"로만 적혀 소수 허용 여부가 불분명하다), `baselineBpm`, `steps`에도 적용돼 4.1의 입력 계약이 바뀐다. 그래서 전역 설정은 쓰지 않고 `global/util/StrictIntegerDeserializer`를 `RecordRequest`의 두 점수 필드에만 붙였다. `@JsonFormat(lenient = OptBoolean.FALSE)`는 record 컴포넌트에서 소수·문자열을 막지 못해 쓰지 않았다.
- `pictureId`, `alertId` 길이: `@Size(max = 255)`로 400 `COMMON-001`을 돌려준다. 존재 여부를 확인하지 않는다는 결정(결정 6)은 그대로다.
