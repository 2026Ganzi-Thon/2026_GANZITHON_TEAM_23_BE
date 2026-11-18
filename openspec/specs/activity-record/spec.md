# activity-record Specification

## Purpose

숨은그림찾기 활동을 마친 뒤 활동 전후의 불안 점수를 기록하고, 사용자가 자기 기록 목록을 최신순으로 보는 규칙을 정한다. 계약 원문은 `docs/API명세서.md` 6장이다.

## Requirements

### Requirement: 활동 기록 저장
서버는 `POST /api/v1/records`로 `pictureId`, `alertId`, `beforeScore`, `afterScore`를 받아 요청한 사용자의 기록으로 저장해야 한다(SHALL). 저장하면 201과 함께 저장된 기록 1건을 돌려준다. 응답 필드는 `recordId`, `activityName`, `pictureId`, `beforeScore`, `afterScore`, `createdAt`이다.

#### Scenario: 정상 저장
- **WHEN** `pictureId` "pic_park_01", `alertId`, `beforeScore` 4, `afterScore` 2로 저장한다
- **THEN** 서버는 201과 새 `recordId`, 보낸 점수, `pictureId` "pic_park_01", `createdAt`을 돌려준다

#### Scenario: 그림과 알림 없이 저장
- **WHEN** `pictureId`, `alertId`, `beforeScore`를 모두 `null`로 보내고 `afterScore`는 3으로 저장한다
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
서버는 `GET /api/v1/records`에 요청한 사용자의 기록만 최신순으로 담아 200으로 돌려줘야 한다(SHALL). 최신순은 `createdAt` 내림차순이고, 시각이 같으면 `recordId` 내림차순이다. 기록이 없으면 빈 배열을 돌려준다. 각 항목의 형태는 저장 응답과 같다. 페이지 나눔은 없다.

#### Scenario: 최신순 목록
- **WHEN** 사용자가 기록 두 건을 차례로 저장한 뒤 목록을 조회한다
- **THEN** 서버는 200과 두 건을 돌려주고, 나중에 저장한 기록이 첫 번째다

#### Scenario: 다른 사용자의 기록은 보이지 않음
- **WHEN** 기록이 없는 다른 사용자가 목록을 조회한다
- **THEN** 서버는 200과 빈 배열을 돌려준다
