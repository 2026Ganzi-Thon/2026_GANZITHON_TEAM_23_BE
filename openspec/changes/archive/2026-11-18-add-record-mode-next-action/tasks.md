# Tasks

## 1. 모드와 다음 행동 타입

- [x] 1.1 `entity/Mode`(`COLOR`, `OBJECT`)와 `entity/NextAction`(`IMPROVED`, `RETRY`, `HELP`) enum을 만든다. `add-picture-next`를 먼저 구현해 `entity/Mode`가 이미 있으면 그것을 쓴다. 확인: `./gradlew compileJava` 성공
- [x] 1.2 `entity/ActivityRecord`에 `mode`(`@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`)와 `nextAction`(`@Enumerated(EnumType.STRING)`, NULL 허용) 필드, 생성자의 `mode` 인자, `recordNextAction(NextAction)`, `getMode()`, `getNextAction()`을 추가한다. 확인: `./gradlew compileJava` 성공

## 2. 기록 저장과 목록에 모드 반영 (명세 6.1, 6.2)

- [x] 2.1 `dto/record/RecordRequest` 맨 앞에 `@NotNull(message = "모드가 필요해요.") Mode mode`를 추가하고, `dto/record/RecordResponse`를 `recordId, activityName, mode, pictureId, beforeScore, afterScore, nextAction, createdAt` 순서로 바꾼다. `service/RecordService.save`가 `mode`를 저장하게 한다. 확인: `./gradlew compileJava` 성공
- [x] 2.2 `PulseFlowTest.saveAndListRecords`의 기존 저장 요청 본문 두 개에 `mode`를 넣는다(예: `"mode":"OBJECT"`). 확인: `./gradlew test`에서 이 테스트가 통과
- [x] 2.3 테스트를 추가한다: `mode` 누락 시 400 `COMMON-001`과 `errors`의 `mode` 항목, `mode` "REAL" 시 400 `COMMON-001`, 저장 응답과 목록 항목에 `mode` 값과 `"nextAction": null` 키가 있음. 확인: `./gradlew test` 통과
- [x] 2.4 `RecordService.DEFAULT_ACTIVITY`의 주석을 "팀 결정(명세 8.1 1번)으로 활동 이름은 고정, 모드는 `mode` 필드로 받는다"는 내용으로 고친다. 확인: 주석에 "모드가 확정되면 바꿀 자리"라는 문구가 남아 있지 않음
- [x] 2.5 `README.md`의 API 구현 현황 표에서 6.1, 6.2 행의 상태를 `완료`로 바꾸고, 맨 위 "현재 상태" 문장의 완료·일부·미구현 개수를 맞춘다. 확인: 두 행의 상태가 `완료`이고 개수가 표와 일치함

## 3. 다음 행동 기록 API (명세 6.3)

- [x] 3.1 `global/exception/ErrorCode`에 `RECORD_001("RECORD-001", HttpStatus.NOT_FOUND, "기록을 찾을 수 없어요.")`를 추가한다. 확인: `./gradlew compileJava` 성공
- [x] 3.2 `repository/RecordRepository.findByIdAndUserId`, `dto/record/NextActionRequest`(`@NotNull(message = "다음 행동이 필요해요.") NextAction action`), `service/RecordService.recordNextAction(userId, recordId, request)`(`@Transactional`, 없으면 `RECORD_001`), `controller/RecordController`의 `POST /{recordId}/next-action`(200, `ApiResponse.message("처리되었습니다.")`)을 구현한다. 확인: `./gradlew compileJava` 성공
- [x] 3.3 테스트를 추가한다: `RETRY` 기록 후 목록의 `nextAction`이 "RETRY", `RETRY` 다음 `HELP`를 보내면 "HELP"로 덮어씀, 없는 `recordId`는 404 `RECORD-001`, 다른 사용자의 기록은 404 `RECORD-001`이고 주인의 목록에서 값이 바뀌지 않음, `{"action":"LATER"}`는 400 `COMMON-001`, `{}`는 400과 `errors`의 `action` 항목, `/api/v1/records/abc/next-action`은 400과 `errors[0].field` "recordId", 토큰 없이 호출하면 401 `AUTH-003`. 확인: `./gradlew test` 통과
- [x] 3.4 `README.md`의 API 구현 현황 표에서 6.3 행의 상태를 `완료`로 바꾸고, 표 아래의 "`RECORD-001`만 아직 없다"는 문장과 맨 위 "현재 상태" 문장의 개수를 고친다. 확인: 6.3 행의 상태가 `완료`이고 `RECORD-001` 미구현 안내가 남아 있지 않음

## 4. 입력 검증 보강 (design.md 미결 사항 해소)

- [x] 4.1 `RecordRequest`의 `beforeScore`·`afterScore`에 `global/util/StrictIntegerDeserializer`(`@JsonDeserialize`)를 붙여 `2.5`·`"3"` 같은 값을 400 `COMMON-001`로 막는다. 전역 Jackson 설정(`accept-float-as-int: false`, `allow-coercion-of-scalars: false`)은 명세 4.1(`/heart-rate/spikes`)의 `bpm` 등 다른 API 입력까지 바꿔 쓰지 않았다. 확인: 소수·문자열·`3.0`·int 범위를 넘는 수 모두 400이고 `./gradlew test` 통과
- [x] 4.2 `RecordRequest`의 `pictureId`·`alertId`에 `@Size(max = 255)`를 붙여 255자 초과를 500이 아닌 400 `COMMON-001`로 막는다. 확인: 255자는 201, 256자는 400이고 `errors`에 해당 필드가 들어 있음, `./gradlew test` 통과
- [x] 4.3 두 보강의 테스트를 추가한다(`recordScoreMustBeIntegerNumber`, `recordIdsOver255CharsAreBadRequestNot500`). 확인: `./gradlew test` 통과
- [x] 4.4 `entity/Mode`·`entity/NextAction`에 이름 문자열만 받는 `@JsonCreator` 정적 팩토리 `from(String)`을 붙여 `0`·`"1"`·`1`·`"2"` 같은 순번을 400 `COMMON-001`로 막는다(Jackson 기본은 enum을 순번으로도 받는다). 전역 `fail-on-numbers-for-enums`는 `"1"`을 못 막아 쓰지 않았다. 확인: `mode` 0·`"1"`, `action` 1·`"2"`가 400 `COMMON-001`이고 저장·변경되지 않음, 소문자는 여전히 400, `GET /api/v1/pictures/next?mode=OBJECT`는 200, `mode=PURPLE`·`mode=0`은 400이며 `./gradlew test` 통과

## 5. 통합 확인

- [x] 5.1 전체 테스트를 실행한다. 확인: `./gradlew test` 전체 통과
- [x] 5.2 `./gradlew bootRun`으로 띄우고 가입, 기록 저장(`mode` 포함), 다음 행동 기록, 목록 조회를 차례로 호출한다. 확인: 목록 응답이 명세 6.1 예시와 같은 필드(`mode`, `nextAction` 포함)를 가짐
