# heart-rate-alert Specification

## Purpose

앱이 보낸 심박 급상승 보고를 보고 알림을 띄울지 서버가 정하는 규칙과, 사용자가 알림을 거절할 때 그 사용자의 알림 기준값을 올리는 규칙을 정한다. 급상승 감지 자체와 푸시 알림은 앱이 맡는다. 계약 원문은 `docs/API명세서.md` 4장이다.

## Requirements

### Requirement: 급상승 보고 입력
서버는 `POST /api/v1/heart-rate/spikes`로 `bpm`, `baselineBpm`, `steps`, `measuredAt`을 받아야 한다(SHALL). `bpm`과 `baselineBpm`은 필수이고 1 이상이다. `measuredAt`은 필수이고 오프셋이 있는 ISO 8601 시각이다(예: `2026-11-15T14:20:00.000Z`). `steps`는 생략하거나 `null`로 보낼 수 있다. 규칙을 어기면 400 `COMMON-001`을 돌려준다.

#### Scenario: bpm 누락
- **WHEN** `bpm` 없이 보고한다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다
- **AND** `errors`에 `bpm` 항목(이유 "bpm이 필요해요.")이 들어 있다

#### Scenario: 걸음 수 생략
- **WHEN** `steps` 필드 없이 다른 값만 보낸다
- **THEN** 서버는 `steps`를 `null`로 보고 200으로 처리한다

### Requirement: 알림 여부 판단
서버는 걸음이 없고 평소 대비 상승폭이 그 사용자의 현재 기준값 이상일 때만 알려야 한다(SHALL). 걸음이 없다는 것은 `steps`가 없거나 `null`이거나 `0`이라는 뜻이다. 상승폭은 `bpm - baselineBpm`이다. 알릴 때는 200과 `shouldAlert: true`, `alertId`를 돌려준다. 알리지 않을 때는 200과 `shouldAlert: false`, `alertId: null`을 돌려준다.

#### Scenario: 걸음 없이 기준값 이상 상승
- **WHEN** 기준값이 25인 사용자가 `bpm` 108, `baselineBpm` 74, `steps` `null`로 보고한다
- **THEN** 서버는 200과 `shouldAlert: true`, `alertId`를 돌려준다

#### Scenario: 상승폭이 기준값과 같음
- **WHEN** 기준값이 25인 사용자가 `bpm` 99, `baselineBpm` 74, `steps` 0으로 보고한다
- **THEN** 서버는 200과 `shouldAlert: true`를 돌려준다

#### Scenario: 상승폭이 기준값보다 작음
- **WHEN** 기준값이 25인 사용자가 `bpm` 90, `baselineBpm` 74, `steps` 0으로 보고한다
- **THEN** 서버는 200과 `shouldAlert: false`, `alertId: null`을 돌려준다

#### Scenario: 걷는 중
- **WHEN** `bpm` 120, `baselineBpm` 74, `steps` 40으로 보고한다
- **THEN** 서버는 200과 `shouldAlert: false`를 돌려준다

### Requirement: 알림 저장과 알림 ID
서버는 알리기로 한 보고만 알림으로 저장하고 `alertId`를 발급해야 한다(SHALL). `alertId`는 `alert_`, 저장 시각의 한국 날짜 8자리(`yyyyMMdd`), `_`, 일련번호를 이어 붙인 문자열이다(예: `alert_20261115_0001`). 일련번호는 4자리가 되도록 앞을 0으로 채우고, 9999를 넘으면 자릿수가 늘어난다. 일련번호는 날짜나 사용자마다 새로 시작하지 않고 서버 전체에서 하나씩 늘어난다.

#### Scenario: 알림 ID 형식
- **WHEN** 알림 조건에 맞는 보고를 한다
- **THEN** 응답의 `alertId`는 정규식 `alert_\d{8}_\d{4,}`에 맞는다

#### Scenario: 알리지 않은 보고
- **WHEN** 알림 조건에 맞지 않는 보고를 한다
- **THEN** 서버는 알림을 저장하지 않고 `alertId`로 `null`을 돌려준다

### Requirement: 사용자별 알림 기준값
서버는 사용자마다 알림 기준값을 따로 가져야 한다(SHALL). 기준값은 가입할 때 25로 시작하고, 알림을 거절할 때마다 5씩 오르며, 45를 넘지 않는다. 세 값은 설정 `pulse.alert.base-threshold`, `pulse.alert.dismiss-step`, `pulse.alert.max-threshold`로 바꿀 수 있다.

#### Scenario: 거절할수록 덜 울림
- **WHEN** 새 사용자가 상승폭 34인 알림을 한 번 거절한 뒤 같은 상승폭으로 다시 보고한다
- **THEN** 기준값이 30이므로 서버는 `shouldAlert: true`를 돌려준다
- **AND** 새로 받은 알림도 거절해 기준값이 35가 되면, 같은 보고에 `shouldAlert: false`를 돌려준다

#### Scenario: 최댓값
- **WHEN** 사용자가 서로 다른 알림을 네 번 거절해 기준값이 45가 된 뒤 또 다른 알림을 거절한다
- **THEN** 기준값은 45에서 더 오르지 않는다

### Requirement: 알림 거절
서버는 `POST /api/v1/alerts/{alertId}/dismiss`를 받으면 요청한 사용자의 알림일 때만 거절로 표시하고 그 사용자의 기준값을 올려야 한다(SHALL). 성공하면 200과 `message: "처리되었습니다."`를 돌려준다. 알림이 없거나 다른 사용자의 알림이면 404 `ALERT-001`과 메시지 "알림을 찾을 수 없어요."를 돌려준다. 요청 본문은 없다.

#### Scenario: 내 알림 거절
- **WHEN** 사용자가 자기 `alertId`로 거절을 요청한다
- **THEN** 서버는 200과 `message: "처리되었습니다."`를 돌려준다

#### Scenario: 없는 알림
- **WHEN** `alert_00000000_9999`로 거절을 요청한다
- **THEN** 서버는 404 `ALERT-001`을 돌려준다

#### Scenario: 다른 사용자의 알림
- **WHEN** 사용자가 다른 사용자의 `alertId`로 거절을 요청한다
- **THEN** 서버는 404 `ALERT-001`을 돌려주고 두 사용자의 기준값은 바뀌지 않는다

### Requirement: 같은 알림의 재거절
서버는 이미 거절한 알림을 다시 거절하면 200을 돌려주되 기준값을 다시 올리지 않아야 한다(SHALL).

#### Scenario: 같은 알림을 두 번 거절
- **WHEN** 사용자가 같은 `alertId`로 거절을 두 번 요청한다
- **THEN** 두 요청 모두 200을 받고 기준값은 5만 오른다

### Requirement: 급상승 보고의 사용자 확인
서버는 급상승 보고를 받았을 때 토큰은 유효하지만 그 사용자가 없으면 401 `AUTH-003`을 돌려줘야 한다(SHALL).

#### Scenario: 메모리 DB가 비워진 뒤 예전 토큰으로 보고
- **WHEN** 로컬 서버를 다시 띄워 메모리 DB가 비워진 뒤, 재시작 전에 받은 토큰으로 급상승을 보고한다
- **THEN** 서버는 401 `AUTH-003`을 돌려준다
