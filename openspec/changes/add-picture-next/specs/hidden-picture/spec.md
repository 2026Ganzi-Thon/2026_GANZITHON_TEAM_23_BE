## Purpose

숨은그림찾기 활동에 쓸 그림 한 장과 찾을 대상의 위치를 모드별로 내려주는 규칙을 정한다. 그림 이미지 파일을 내려받는 경로와 그림 데이터의 형식 규칙도 여기에 속한다. 계약 원문은 `docs/API명세서.md` 5.1절이다.

## ADDED Requirements

### Requirement: 숨은그림 받기
서버는 `GET /api/v1/pictures/next`에 200과 함께 그림 한 장의 `pictureId`, `mode`, `imageUrl`, `aspectRatio`, `targets`를 돌려줘야 한다(SHALL). `targets`의 각 항목은 `targetId`, `label`, `emoji`, `x`, `y`, `radius`를 가진다. `aspectRatio`는 이미지 너비를 높이로 나눈 값이다(예: 850×1850 이미지는 0.4595). 이 API는 인증이 필요하다.

#### Scenario: 사물 모드 그림 받기
- **WHEN** 유효한 토큰으로 `GET /api/v1/pictures/next?mode=OBJECT`를 호출한다
- **THEN** 서버는 200을 돌려주고 `data.mode`는 "OBJECT"다
- **AND** `data.pictureId`, `data.imageUrl`은 빈 문자열이 아니고, `data.targets`는 1개 이상이며 각 항목에 여섯 필드가 모두 있다

#### Scenario: 토큰 없이 호출
- **WHEN** 토큰 없이 `GET /api/v1/pictures/next?mode=OBJECT`를 호출한다
- **THEN** 서버는 401 `AUTH-003`을 돌려준다

### Requirement: 모드별 그림 선택
서버는 요청한 `mode`와 같은 모드의 그림 중 하나를 무작위로 골라 돌려줘야 한다(SHALL). 응답의 `mode`는 요청한 값과 같다. 같은 모드의 그림이 한 장뿐이면 매번 그 그림을 돌려준다. 같은 모드의 그림이 여러 장이면 호출할 때마다 다른 그림이 나올 수 있다.

#### Scenario: 요청한 모드의 그림만 나옴
- **WHEN** 사물 모드 그림과 색 모드 그림이 모두 있을 때 `mode=COLOR`로 여러 번 호출한다
- **THEN** 서버는 매번 200을 돌려주고 모든 응답의 `mode`는 "COLOR"다

### Requirement: 모드 파라미터 검증
서버는 `mode` 쿼리 파라미터를 필수로 받아야 하며 값은 대문자 `COLOR` 또는 `OBJECT`여야 한다(SHALL). `mode`가 없으면 400 `COMMON-001`과 빈 `errors`를 돌려준다. 두 값이 아니면 400 `COMMON-001`과 `errors`의 `mode` 항목을 돌려준다.

#### Scenario: 모드 누락
- **WHEN** 유효한 토큰으로 `GET /api/v1/pictures/next`를 호출한다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다

#### Scenario: 정의되지 않은 모드
- **WHEN** 유효한 토큰으로 `GET /api/v1/pictures/next?mode=PURPLE`을 호출한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors[0].field`는 "mode"다

### Requirement: 알림 ID는 선택 값
서버는 `alertId` 쿼리 파라미터 없이도 요청을 처리해야 하며, 값이 와도 그런 알림이 있는지 확인하지 않아야 한다(SHALL). `alertId`는 그림 선택에 영향을 주지 않는다. 알림에서 시작한 활동이든 홈에서 바로 시작한 활동이든 흐름을 막지 않기 위해서다.

#### Scenario: 알림 없이 시작
- **WHEN** `alertId` 없이 `mode=OBJECT`로 호출한다
- **THEN** 서버는 200을 돌려준다

#### Scenario: 없는 알림 ID
- **WHEN** 저장된 적 없는 `alertId=alert_00000000_9999`와 `mode=OBJECT`로 호출한다
- **THEN** 서버는 200을 돌려준다

### Requirement: 내려줄 그림이 없음
서버는 요청한 모드의 그림이 하나도 없으면 404 `PICTURE-001`과 메시지 "그림을 불러오지 못했어요."를 돌려줘야 한다(SHALL).

#### Scenario: 색 모드 그림이 없는 상태
- **WHEN** 색 모드 그림이 등록되지 않은 상태에서 `mode=COLOR`로 호출한다
- **THEN** 서버는 404 `PICTURE-001`을 돌려준다
- **AND** 응답 본문의 `status`는 404, `code`는 "PICTURE-001", `message`는 "그림을 불러오지 못했어요."이고 `errors`는 빈 배열이다

### Requirement: 찾을 대상 좌표 형식
서버가 내려주는 `targets`는 1개 이상이어야 하고, 각 대상의 위치는 픽셀이 아닌 비율로 표현해야 한다(SHALL). `x`는 이미지 너비 기준, `y`는 이미지 높이 기준의 대상 중심 위치이고 둘 다 0 이상 1 이하다. `radius`는 정답으로 인정할 반경이며 가로와 세로 모두 이미지 너비 기준 비율로 0 초과 1 이하다. `targetId`는 한 그림 안에서 서로 다르고, 같은 `label`은 여러 대상에 쓸 수 있다.

#### Scenario: 응답 좌표 범위
- **WHEN** `mode=OBJECT`로 그림을 받는다
- **THEN** 모든 대상의 `x`, `y`는 0 이상 1 이하이고 `radius`는 0 초과 1 이하다
- **AND** `targetId`는 서로 다르다

### Requirement: 그림 데이터 검증
서버는 기동할 때 그림 데이터 전체를 검사하고, 규칙을 어긴 데이터가 있으면 기동하지 않아야 한다(MUST). 규칙은 다음과 같다. `pictureId`는 전체에서 고유하다. `mode`는 `COLOR` 또는 `OBJECT`다. `aspectRatio`는 0보다 크다. `targets`는 1개 이상이다. 각 대상의 `x`, `y`는 0 이상 1 이하이고 `radius`는 0 초과 1 이하다. 각 대상의 `label`과 `emoji`는 비어 있지 않다. `targetId`는 그림 안에서 고유하다. 실패 메시지에는 문제가 된 `pictureId`와 어긴 규칙을 적는다.

#### Scenario: 범위를 벗어난 좌표
- **WHEN** 어떤 대상의 `x`가 1.2인 그림 데이터로 서버를 띄운다
- **THEN** 서버는 기동에 실패하고 오류 메시지에 그 그림의 `pictureId`가 들어 있다

#### Scenario: 같은 대상 ID
- **WHEN** 한 그림 안에 `targetId`가 같은 대상이 두 개 있는 데이터로 서버를 띄운다
- **THEN** 서버는 기동에 실패한다

#### Scenario: 반경이 0이거나 없음
- **WHEN** 어떤 대상의 `radius`가 0이거나 JSON에서 `radius` 키가 빠진 그림 데이터로 서버를 띄운다
- **THEN** 서버는 기동에 실패하고 오류 메시지에 그 그림의 `pictureId`가 들어 있다

#### Scenario: 이름이나 이모지가 빈 대상
- **WHEN** 어떤 대상의 `label` 또는 `emoji`가 비어 있는 그림 데이터로 서버를 띄운다
- **THEN** 서버는 기동에 실패하고 오류 메시지에 그 그림의 `pictureId`가 들어 있다

#### Scenario: 대상이 없는 그림
- **WHEN** `targets`가 빈 배열인 그림 데이터로 서버를 띄운다
- **THEN** 서버는 기동에 실패한다

### Requirement: 그림 이미지 제공
서버는 그림 이미지 파일을 `/pictures/{파일 이름}` 경로로 토큰 없이 내려줘야 한다(SHALL). 응답의 `imageUrl`은 이 파일을 가리키는 절대 URL이다. 설정 `pulse.public-base-url`에 값이 있으면 그 값을 기준 주소로 쓰고, 없으면 요청을 받은 서버의 스킴, 호스트, 포트를 쓴다. `prod` 프로필은 `PULSE_PUBLIC_BASE_URL`을 기본값 없이 요구해, 값이 없으면 기동하지 않는다(프록시 뒤에서 `Host` 헤더가 `imageUrl`에 들어가는 것을 막기 위해서다). 이 경로는 `/api/v1` 밖이라 인증하지 않는다.

#### Scenario: 기준 주소 설정이 없을 때
- **WHEN** 설정 없이 `http://localhost:8080`으로 받은 요청에서 파일 이름이 `park_01.png`인 그림을 고른다
- **THEN** `imageUrl`은 `http://localhost:8080/pictures/park_01.png`다

#### Scenario: 기준 주소 설정이 있을 때
- **WHEN** `pulse.public-base-url`이 `https://api.example.com`이고 파일 이름이 `park_01.png`인 그림을 고른다
- **THEN** `imageUrl`은 `https://api.example.com/pictures/park_01.png`다

#### Scenario: prod에서 기준 주소 설정 누락
- **WHEN** `prod` 프로필로 환경변수 `PULSE_PUBLIC_BASE_URL` 없이 서버를 띄운다
- **THEN** 서버는 기동에 실패한다

#### Scenario: 토큰 없이 이미지 받기
- **WHEN** 토큰 없이 `imageUrl`에 GET 요청을 보낸다
- **THEN** 서버는 200과 이미지 파일을 돌려준다

#### Scenario: 없는 이미지 파일
- **WHEN** 토큰 없이 `/pictures/none.png`를 요청한다
- **THEN** 서버는 404 `COMMON-001`을 돌려준다
