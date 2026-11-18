# activity-record Specification

## Purpose

숨은그림찾기 활동을 마친 뒤 활동 전후의 불안 점수를 기록하고, 사용자가 자기 기록 목록을 최신순으로 보는 규칙을 정한다. 계약 원문은 `docs/API명세서.md` 6장이다.

## Requirements

### Requirement: 활동 기록 저장
서버는 `POST /api/v1/records`로 `mode`, `pictureId`, `alertId`, `beforeScore`, `afterScore`를 받아 요청한 사용자의 기록으로 저장해야 한다(SHALL). 저장하면 201과 함께 저장된 기록 1건을 돌려준다. 응답 필드는 `recordId`, `activityName`, `mode`, `pictureId`, `beforeScore`, `afterScore`, `nextAction`, `createdAt`이다. 막 저장한 기록의 `nextAction`은 `null`이다.

#### Scenario: 정상 저장
- **WHEN** `mode` "OBJECT", `pictureId` "pic_park_01", `alertId`, `beforeScore` 4, `afterScore` 2로 저장한다
- **THEN** 서버는 201과 새 `recordId`, `mode` "OBJECT", 보낸 점수, `pictureId` "pic_park_01", `createdAt`을 돌려준다
- **AND** 응답의 `nextAction`은 `null`이다

#### Scenario: 그림과 알림 없이 저장
- **WHEN** `mode`는 "COLOR"로, `pictureId`, `alertId`, `beforeScore`는 모두 `null`로, `afterScore`는 3으로 저장한다
- **THEN** 서버는 201을 돌려주고 응답의 `pictureId`와 `beforeScore`는 `null`이다

### Requirement: 활동 이름 고정
서버는 `activityName`을 요청으로 받지 않고 항상 "숨은그림찾기"로 저장하고 돌려줘야 한다(SHALL).

#### Scenario: 저장된 활동 이름
- **WHEN** 기록을 저장한다
- **THEN** 응답의 `activityName`은 "숨은그림찾기"다

### Requirement: 활동 점수 규칙
서버는 `afterScore`를 필수로 받고 1~5 범위인지 확인해야 한다(SHALL). `beforeScore`는 사용자가 건너뛰면 `null`이고, 값이 있으면 1~5 범위여야 한다. 규칙을 어기면 400 `COMMON-001`을 돌려준다. 1은 "편안해요", 5는 "많이 불안해요"를 뜻한다.

#### Scenario: 범위를 벗어난 점수
- **WHEN** `afterScore`를 6으로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`의 이유는 "점수는 1~5예요."다

#### Scenario: 활동 후 점수 누락
- **WHEN** `afterScore` 없이 저장을 요청한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`의 이유는 "활동 후 점수가 필요해요."다

#### Scenario: 활동 전 점수 0
- **WHEN** `beforeScore`를 0으로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다

### Requirement: 그림 ID와 알림 ID의 저장
서버는 `pictureId`와 `alertId`를 `null`을 허용해 받고, 그런 그림이나 알림이 실제로 있는지 확인하지 않고 받은 값 그대로 저장해야 한다(SHALL). `alertId`는 저장만 하고 응답에는 넣지 않는다.

#### Scenario: 없는 알림 ID로 저장
- **WHEN** 저장된 적 없는 `alertId` "alert_00000000_9999"로 기록을 저장한다
- **THEN** 서버는 201을 돌려준다

### Requirement: 활동 기록 목록
서버는 `GET /api/v1/records`에 요청한 사용자의 기록만 최신순으로 담아 200으로 돌려줘야 한다(SHALL). 최신순은 `createdAt` 내림차순이고, 시각이 같으면 `recordId` 내림차순이다. 기록이 없으면 빈 배열을 돌려준다. 각 항목의 형태는 저장 응답과 같으며 `mode`와 현재 `nextAction`을 담는다. 페이지 나눔은 없다.

#### Scenario: 최신순 목록
- **WHEN** 사용자가 기록 두 건을 차례로 저장한 뒤 목록을 조회한다
- **THEN** 서버는 200과 두 건을 돌려주고, 나중에 저장한 기록이 첫 번째다

#### Scenario: 다른 사용자의 기록은 보이지 않음
- **WHEN** 기록이 없는 다른 사용자가 목록을 조회한다
- **THEN** 서버는 200과 빈 배열을 돌려준다

#### Scenario: 목록에 모드와 다음 행동 표시
- **WHEN** 사용자가 `mode` "OBJECT"로 저장한 기록에 다음 행동 `IMPROVED`를 기록하고, `mode` "COLOR"로 기록을 하나 더 저장한 뒤 목록을 조회한다
- **THEN** 첫 번째 항목은 `mode` "COLOR", `nextAction` `null`이다
- **AND** 두 번째 항목은 `mode` "OBJECT", `nextAction` "IMPROVED"다

### Requirement: 활동 모드 필수
서버는 기록 저장 요청의 `mode`를 필수로 받아야 하며, 값은 대문자 `COLOR`(색 모드) 또는 `OBJECT`(사물 모드)여야 한다(SHALL). `mode`가 없거나 `null`이면 400 `COMMON-001`과 `errors`의 `mode` 항목(이유 "모드가 필요해요.")을 돌려준다. 두 값이 아닌 문자열이나 `0`, `"1"` 같은 순번(숫자와 숫자 문자열)이면 본문을 읽을 수 없는 것으로 보고 400 `COMMON-001`과 빈 `errors`를 돌려준다. 소문자(`"color"`)도 받지 않는다.

#### Scenario: 모드 누락
- **WHEN** `mode` 없이 `afterScore` 3만 보내 저장한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`에 `mode` 항목이 들어 있다

#### Scenario: 정의되지 않은 모드
- **WHEN** `mode`를 "REAL"로 보내 저장한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`는 빈 배열이다

#### Scenario: 숫자 모드
- **WHEN** `mode`를 숫자 0으로 보내 저장한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 기록을 저장하지 않는다

#### Scenario: 숫자 문자열 모드
- **WHEN** `mode`를 문자열 "1"로 보내 저장한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 기록을 저장하지 않는다

### Requirement: 다음 행동 기록
서버는 `POST /api/v1/records/{recordId}/next-action`으로 본문 `{"action": ...}`을 받아 그 기록의 `nextAction`에 저장해야 한다(SHALL). `action` 값은 `IMPROVED`(나아졌어요), `RETRY`(한 번 더), `HELP`(도움 요청) 중 하나다. 성공하면 200과 `message: "처리되었습니다."`를 돌려준다.

#### Scenario: 한 번 더를 고름
- **WHEN** 사용자가 자기 기록에 `{"action": "RETRY"}`를 보낸다
- **THEN** 서버는 200과 `message: "처리되었습니다."`를 돌려준다
- **AND** 이후 목록에서 그 기록의 `nextAction`은 "RETRY"다

### Requirement: 다음 행동 덮어쓰기
서버는 다음 행동이 이미 기록된 기록에 다시 요청이 오면 마지막에 받은 값으로 덮어써야 한다(SHALL). 이 경우에도 200을 돌려준다.

#### Scenario: 두 번 기록
- **WHEN** 사용자가 같은 기록에 `RETRY`를 보낸 뒤 `HELP`를 보낸다
- **THEN** 두 요청 모두 200을 받는다
- **AND** 목록에서 그 기록의 `nextAction`은 "HELP"다

### Requirement: 다음 행동 기록 대상 확인
서버는 `recordId`에 해당하는 기록이 요청한 사용자의 것일 때만 다음 행동을 기록해야 한다(SHALL). 기록이 없거나 다른 사용자의 기록이면 404 `RECORD-001`과 메시지 "기록을 찾을 수 없어요."를 돌려주고 아무것도 바꾸지 않는다.

#### Scenario: 없는 기록
- **WHEN** 존재하지 않는 `recordId`로 다음 행동을 보낸다
- **THEN** 서버는 404 `RECORD-001`을 돌려준다

#### Scenario: 다른 사용자의 기록
- **WHEN** 사용자가 다른 사용자의 `recordId`로 다음 행동을 보낸다
- **THEN** 서버는 404 `RECORD-001`을 돌려준다
- **AND** 그 기록의 주인이 목록을 조회하면 `nextAction`은 바뀌지 않았다

### Requirement: 다음 행동 입력 검증
서버는 다음 행동 요청의 입력이 잘못되면 400 `COMMON-001`을 돌려줘야 한다(SHALL). `action`이 없거나 `null`이면 `errors`에 `action` 항목(이유 "다음 행동이 필요해요.")을 담는다. `action`이 세 값이 아닌 문자열이거나 `1`, `"2"` 같은 순번(숫자와 숫자 문자열)이면 `errors`는 빈 배열이다. `recordId`가 숫자가 아니면 `errors`에 `recordId` 항목을 담는다.

#### Scenario: 정의되지 않은 행동
- **WHEN** `{"action": "LATER"}`를 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다

#### Scenario: 숫자 행동
- **WHEN** `{"action": 1}`을 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 기록의 `nextAction`은 바뀌지 않았다

#### Scenario: 숫자 문자열 행동
- **WHEN** `{"action": "2"}`를 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 기록의 `nextAction`은 바뀌지 않았다

#### Scenario: 행동 누락
- **WHEN** 빈 객체 `{}`를 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`에 `action` 항목이 들어 있다

#### Scenario: 숫자가 아닌 기록 ID
- **WHEN** `POST /api/v1/records/abc/next-action`을 호출한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors[0].field`는 "recordId"다

### Requirement: 활동 점수는 정수
서버는 `beforeScore`와 `afterScore`를 JSON 정수로만 받아야 한다(SHALL). `2.5` 같은 소수, `3.0`, `"3"` 같은 문자열, int 범위를 넘는 수는 저장하지 않고 400 `COMMON-001`과 빈 `errors`를 돌려준다. 이 규칙은 기록 저장 요청의 두 점수 필드에만 적용한다.

#### Scenario: 소수 점수
- **WHEN** `afterScore`를 2.5로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 기록을 저장하지 않는다

#### Scenario: 문자열 점수
- **WHEN** `beforeScore`를 문자열 "3"으로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 기록을 저장하지 않는다

### Requirement: 그림 ID와 알림 ID 길이 제한
서버는 `pictureId`와 `alertId`가 255자를 넘으면 400 `COMMON-001`과 `errors`의 해당 필드 항목을 돌려줘야 한다(SHALL). 255자까지는 존재 여부를 확인하지 않고 받은 값 그대로 저장한다.

#### Scenario: 255자 그림 ID
- **WHEN** `pictureId`와 `alertId`를 각각 255자로 저장한다
- **THEN** 서버는 201을 돌려준다

#### Scenario: 255자를 넘는 그림 ID
- **WHEN** `pictureId`를 256자로 보내 저장한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`에 `pictureId` 항목이 들어 있다

#### Scenario: 255자를 넘는 알림 ID
- **WHEN** `alertId`를 256자로 보내 저장한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`에 `alertId` 항목이 들어 있다
