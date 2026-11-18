# api-common Specification

## Purpose

모든 API가 함께 따르는 규칙을 정한다. 성공 응답과 오류 응답의 형식, 공통 오류 코드, 잘못된 요청의 처리, JWT 인증, CORS, 시각 형식이 여기에 속한다. 계약 원문은 `docs/API명세서.md` 2장이다.

## Requirements

### Requirement: 성공 응답 형식
서버는 요청을 성공적으로 처리하면 `success: true`를 담은 JSON으로 응답해야 한다(SHALL). 돌려줄 데이터가 있으면 `data`에 담는다. 데이터 없이 결과만 알릴 때는 `message`에 문구를 담는다. 쓰지 않는 쪽 키(`data` 또는 `message`)는 응답에 넣지 않는다.

#### Scenario: 데이터를 돌려주는 성공
- **WHEN** 로그인한 사용자가 `GET /api/v1/records`를 호출한다
- **THEN** 서버는 200과 `{"success": true, "data": [...]}`를 돌려준다
- **AND** 응답에 `message` 키가 없다

#### Scenario: 결과만 알리는 성공
- **WHEN** 사용자가 자기 알림에 `POST /api/v1/alerts/{alertId}/dismiss`를 호출한다
- **THEN** 서버는 200과 `{"success": true, "message": "처리되었습니다."}`를 돌려준다
- **AND** 응답에 `data` 키가 없다

### Requirement: 응답 데이터의 빈 값 표기
서버는 `data` 안의 필드에 값이 없으면 그 키를 빼지 않고 `null`로 내려줘야 한다(SHALL).

#### Scenario: 알리지 않은 급상승 보고
- **WHEN** 알림 조건에 맞지 않는 급상승 보고를 받는다
- **THEN** 서버는 200을 돌려주고 응답 `data`는 `{"shouldAlert": false, "alertId": null}`이다

#### Scenario: 활동 전 점수를 건너뛴 기록
- **WHEN** `pictureId`와 `beforeScore`를 `null`로 보내 기록을 저장한다
- **THEN** 서버는 201을 돌려주고 응답 `data`에 `"pictureId": null`과 `"beforeScore": null`이 들어 있다

### Requirement: 오류 응답 형식
서버는 오류가 나면 HTTP 상태 코드와 함께 `timestamp`, `status`, `code`, `message`, `errors`, `path` 여섯 필드를 가진 JSON을 돌려줘야 한다(SHALL). `status`는 HTTP 상태 코드와 같은 값이다. `message`는 사용자에게 그대로 보여 줄 수 있는 한국어 문구다. `errors`는 필드별 오류 목록이고, 없으면 빈 배열이다. `path`는 요청 경로다.

#### Scenario: 업무 오류
- **WHEN** 사용자가 없는 알림 ID로 `POST /api/v1/alerts/alert_00000000_9999/dismiss`를 호출한다
- **THEN** 서버는 404를 돌려준다
- **AND** 본문은 `status` 404, `code` `ALERT-001`, `message` "알림을 찾을 수 없어요.", `errors` 빈 배열, `path` `/api/v1/alerts/alert_00000000_9999/dismiss`이다

### Requirement: 공통 오류 코드
서버는 모든 API에서 공통 오류 코드 두 개를 써야 한다(SHALL). `COMMON-001`은 입력 형식 오류에 쓰고 메시지는 "입력값을 확인해 주세요."다. 기본 HTTP 상태는 400이며, 없는 경로와 지원하지 않는 요청 방식에는 아래 요구사항의 상태 코드와 함께 쓴다. `COMMON-002`는 예상하지 못한 서버 오류에 쓰고 HTTP 상태는 500이다. 기능별 오류 코드는 `영역-번호` 형식(예: `AUTH-001`)으로 각 기능 스펙에 정의한다.

#### Scenario: 입력 형식 오류
- **WHEN** 클라이언트가 규칙에 맞지 않는 값을 보낸다
- **THEN** 서버는 400 `COMMON-001`과 메시지 "입력값을 확인해 주세요."를 돌려준다

#### Scenario: 예상하지 못한 서버 오류
- **WHEN** 요청을 처리하다가 예상하지 못한 예외가 난다
- **THEN** 서버는 500 `COMMON-002`와 메시지 "서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요."를 돌려준다
- **AND** 예외 내용은 응답에 넣지 않고 서버 로그에만 남긴다

### Requirement: 입력 검증 실패
서버는 요청 본문의 필드가 검증 규칙을 어기면 400 `COMMON-001`을 돌려주고, `errors`에 어긴 필드마다 `field`, `value`, `reason`을 담아야 한다(SHALL). `value`는 클라이언트가 보낸 값이고 `reason`은 한국어 안내 문구다. `password` 필드의 값은 응답에 담으면 안 되며(MUST NOT), 이 경우 `value`는 `null`이다.

#### Scenario: 여러 필드가 규칙을 어긴 가입 요청
- **WHEN** `POST /api/v1/auth/signup`에 `email` "hello@", `loginId` "x", `password` "1"을 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다
- **AND** `errors`에 `email`(값 "hello@", 이유 "이메일 형식이 아닙니다."), `loginId`(값 "x"), `password` 항목이 들어 있다
- **AND** `password` 항목의 `value`는 `null`이다

### Requirement: 읽을 수 없는 요청
서버는 요청 본문을 JSON으로 읽을 수 없으면 400 `COMMON-001`과 빈 `errors`를 돌려줘야 한다(SHALL). JSON 문법 오류, 본문 없음, 필드 값을 정해진 타입으로 바꿀 수 없는 경우가 여기에 속한다. 경로 변수나 쿼리 파라미터 값을 정해진 타입으로 바꿀 수 없을 때도 400 `COMMON-001`을 돌려준다. 이때는 `errors`에 파라미터 이름, 보낸 값, 이유 "값의 형식이 올바르지 않아요."를 담는다.

#### Scenario: 깨진 JSON
- **WHEN** 유효한 토큰으로 `POST /api/v1/records`에 `{bad`를 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`는 빈 배열이다

#### Scenario: 본문 없음
- **WHEN** 유효한 토큰으로 본문 없이 `POST /api/v1/records`를 호출한다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다

#### Scenario: 날짜로 읽을 수 없는 값
- **WHEN** `POST /api/v1/heart-rate/spikes`에 `measuredAt`을 "yesterday"로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`는 빈 배열이다

#### Scenario: 숫자 파라미터에 문자를 보냄
- **WHEN** 숫자를 받는 쿼리 파라미터 `n`에 "abc"를 보낸다
- **THEN** 서버는 500이 아닌 400 `COMMON-001`을 돌려준다
- **AND** `errors[0].field`는 "n"이다

### Requirement: 없는 경로와 지원하지 않는 요청 방식
서버는 없는 경로에 404를, 경로는 있지만 지원하지 않는 HTTP 메서드에 405를, 지원하지 않는 `Content-Type`에 415를 돌려줘야 한다(SHALL). 세 경우 모두 오류 코드는 `COMMON-001`, 메시지는 "입력값을 확인해 주세요.", `errors`는 빈 배열이다.

#### Scenario: 없는 경로
- **WHEN** 유효한 토큰으로 `GET /api/v1/nope`를 호출한다
- **THEN** 서버는 404 `COMMON-001`을 돌려준다

#### Scenario: 토큰 없이 부른 없는 경로
- **WHEN** 토큰 없이 `GET /api/v1/nope`를 호출한다
- **THEN** 인증 검사가 먼저 실패해 서버는 401 `AUTH-003`을 돌려준다

#### Scenario: 지원하지 않는 메서드
- **WHEN** `DELETE /api/v1/records`를 호출한다
- **THEN** 서버는 토큰이 있든 없든 405 `COMMON-001`을 돌려준다

#### Scenario: JSON이 아닌 본문
- **WHEN** 유효한 토큰으로 `Content-Type: text/plain` 본문을 `POST /api/v1/records`에 보낸다
- **THEN** 서버는 415 `COMMON-001`을 돌려준다

### Requirement: JWT 인증
서버는 `/api/v1/auth/**`를 뺀 모든 `/api/v1/**` 요청에 `Authorization: Bearer {accessToken}` 헤더를 요구해야 한다(SHALL). 헤더가 없거나, 값이 `Bearer `로 시작하지 않거나, 토큰 서명이 맞지 않거나, 토큰이 만료됐으면 401 `AUTH-003`과 메시지 "다시 로그인해 주세요."를 돌려준다. 인증에 성공하면 토큰에 담긴 사용자 ID로 요청을 처리한다. `/api/v1` 밖의 경로와 CORS 사전 요청(`OPTIONS`)은 인증하지 않는다.

#### Scenario: 토큰 없음
- **WHEN** 토큰 없이 `GET /api/v1/records`를 호출한다
- **THEN** 서버는 401 `AUTH-003`을 돌려준다

#### Scenario: 위조한 토큰
- **WHEN** 서명이 맞지 않는 토큰으로 `GET /api/v1/records`를 호출한다
- **THEN** 서버는 401 `AUTH-003`을 돌려준다

#### Scenario: 인증 방식 이름이 다름
- **WHEN** `Authorization` 값을 `bearer {accessToken}`처럼 소문자로 시작해 보낸다
- **THEN** 서버는 401 `AUTH-003`을 돌려준다

#### Scenario: 인증이 필요 없는 경로
- **WHEN** 토큰 없이 `POST /api/v1/auth/signup`이나 로컬의 `/h2-console`을 호출한다
- **THEN** 서버는 인증 오류 없이 요청을 처리한다

### Requirement: CORS
서버는 `/api/**` 경로에서 설정 `pulse.cors.allowed-origin-patterns`에 맞는 Origin의 교차 출처 요청을 허용해야 한다(SHALL). 설정은 쉼표로 여러 개를 줄 수 있고 로컬 기본값은 `*`다. 허용 메서드는 GET, POST, PUT, PATCH, DELETE, OPTIONS이고 요청 헤더는 모두 허용한다. 토큰을 헤더로 보내므로 쿠키 같은 자격 증명은 허용하지 않는다.

#### Scenario: 사전 요청
- **WHEN** 토큰 없이 `OPTIONS /api/v1/records`에 `Origin: http://localhost:3000`과 `Access-Control-Request-Method: GET` 헤더를 보낸다
- **THEN** 서버는 200과 `Access-Control-Allow-Origin` 헤더를 돌려준다

### Requirement: 시각 형식
서버는 응답에 들어가는 모든 시각을 한국 시간(`+09:00`) 기준 ISO 8601 형식, 초 단위로 내려줘야 한다(SHALL). 예: `2026-11-15T14:20:00+09:00`. 오류 응답의 `timestamp`도 같은 형식이다.

#### Scenario: 기록 시각
- **WHEN** 기록을 저장한 뒤 `GET /api/v1/records`로 목록을 조회한다
- **THEN** 서버는 200을 돌려주고 `createdAt`은 `yyyy-MM-ddTHH:mm:ss+09:00` 형태이며 초 아래 자리가 없다
