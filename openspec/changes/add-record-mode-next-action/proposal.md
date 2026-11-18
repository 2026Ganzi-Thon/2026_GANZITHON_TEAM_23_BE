# Proposal

## Why

`docs/API명세서.md` v0.2는 활동 기록에 모드(2.5 `Mode`)와 다음 행동(2.5 `NextAction`)을 넣기로 했다(8.1 팀 결정 2번, 6번). 지금 서버는 6.1 목록과 6.2 저장에 `mode`와 `nextAction`이 없고, 6.3 다음 행동 기록 API도 없다. 프론트는 이미 v0.2 계약대로 `mode`를 보내지만 서버가 그 값을 버리고 있다. 기록 화면(Figma 11번)은 모드 칩을 보여 주고, "다음은 어떻게 할까요?" 화면(Figma 10번)은 사용자가 고른 행동을 서버에 남겨야 하므로 지금 맞춘다.

대응 절: 2.5 공통 타입(`Mode`, `NextAction`), 2.4 `RECORD-001`, 6.1 활동 기록 목록, 6.2 활동 기록 저장, 6.3 다음 행동 기록.

## What Changes

- 6.2 `POST /api/v1/records` 요청에 `mode`(`COLOR` 또는 `OBJECT`)를 필수로 추가한다. **BREAKING**: `mode`를 보내지 않는 클라이언트는 400 `COMMON-001`을 받는다. v0.2를 반영한 프론트는 이미 보내고 있다.
- 6.1 목록과 6.2 저장 응답의 각 기록에 `mode`와 `nextAction`을 추가한다. 다음 행동을 고르기 전에는 `nextAction`이 `null`로 내려간다.
- 6.3 `POST /api/v1/records/{recordId}/next-action`을 새로 만든다. 본문 `{action}`의 값은 `IMPROVED`, `RETRY`, `HELP` 중 하나다. 같은 기록에 다시 보내면 마지막 값으로 덮어쓴다.
- 오류 코드 `RECORD-001`(404, "기록을 찾을 수 없어요.")을 추가한다. 기록이 없거나 다른 사용자의 기록일 때 쓴다.
- `pictureId`와 `alertId`는 지금처럼 존재 여부를 확인하지 않고 받은 값 그대로 저장한다. 명세상 프론트는 저장에 실패해도 다음 화면으로 넘어가므로 서버가 흐름을 막지 않는다.

## Capabilities

### New Capabilities

없음.

### Modified Capabilities

- `activity-record`: 기록 저장 요청에 `mode`가 필수로 들어가고, 저장·목록 응답에 `mode`와 `nextAction`이 추가된다. 다음 행동 기록 API와 `RECORD-001` 오류가 추가된다.

## Impact

- API: `POST /api/v1/records`(요청·응답 변경), `GET /api/v1/records`(응답 변경), `POST /api/v1/records/{recordId}/next-action`(추가)
- 코드: `entity/ActivityRecord`, `dto/record/*`, `repository/RecordRepository`, `service/RecordService`, `controller/RecordController`, `global/exception/ErrorCode`. 새 enum `entity/Mode`, `entity/NextAction`
- DB: `activity_records` 테이블에 `mode`(NOT NULL), `next_action`(NULL 허용) 열이 생긴다. 로컬은 `create-drop`이라 따로 할 일이 없다. 운영 DB는 design.md의 Risks를 본다.
- 테스트: `PulseFlowTest`의 기존 기록 저장 요청 본문에 `mode`를 넣어야 기존 테스트가 통과한다.
- 의존성 추가 없음.
- `entity/Mode`는 `add-picture-next`(5.1 숨은그림 받기)도 쓴다. 먼저 구현하는 쪽이 만든다.

## 범위 밖

- "다른 모드" 선택은 서버에 기록하지 않는다. 명세 6.3 표대로 앱이 다른 `mode`로 5.1을 다시 호출한다.
- 도움 요청(`HELP`)의 전화와 문자 연결은 앱이 휴대폰 기본 앱으로 처리한다(명세 6.3). 서버는 선택만 기록한다.
- Figma 08 현실 미션 단계는 팀 결정(8.1 1번)으로 제외했으므로 기록에 남기지 않는다.
- 다음 행동을 지우는 API는 만들지 않는다. 명세에 없다.
- `pictureId`와 `alertId`의 존재 확인은 하지 않는다. 길이 제한 같은 다른 검증은 design.md의 미결 사항으로 남긴다.
