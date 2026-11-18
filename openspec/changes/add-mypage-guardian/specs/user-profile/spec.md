## Purpose

로그인한 사용자가 자기 프로필(이름, 닉네임, 이메일, 아이디)과 등록한 보호자를 조회하고, 프로필의 일부를 고치는 규칙을 정한다. 계약 원문은 `docs/API명세서.md` 7.1절과 7.2절이다.

## ADDED Requirements

### Requirement: 내 정보 조회
서버는 `GET /api/v1/users/me`에 200과 함께 요청한 사용자의 `userId`, `name`, `nickname`, `email`, `loginId`, `guardian`을 돌려줘야 한다(SHALL). `guardian`은 보호자가 있으면 `{"name": ..., "phone": ...}`이고 없으면 `null`이다. `phone`에는 숫자만 들어 있다.

#### Scenario: 보호자가 없는 사용자
- **WHEN** 가입만 한 사용자가 `GET /api/v1/users/me`를 호출한다
- **THEN** 서버는 200과 가입 때 값을 돌려준다
- **AND** 응답에 `"guardian": null`이 들어 있다

#### Scenario: 보호자가 있는 사용자
- **WHEN** 보호자를 이름 "엄마", 번호 "010-1234-5678"로 등록한 사용자가 내 정보를 조회한다
- **THEN** 서버는 200을 돌려주고 `guardian`은 `{"name": "엄마", "phone": "01012345678"}`이다

### Requirement: 내 정보 부분 수정
서버는 `PATCH /api/v1/users/me`로 받은 `name`, `nickname`, `email` 중 보낸 필드만 바꿔야 한다(SHALL). 보내지 않았거나 `null`로 보낸 필드는 바꾸지 않는다. 성공하면 200과 함께 내 정보 조회와 같은 형태를 돌려준다. `loginId`와 `password`는 이 API로 바꿀 수 없고, 본문에 있어도 무시한다.

#### Scenario: 닉네임만 수정
- **WHEN** `{"nickname": "새닉네임"}`을 보낸다
- **THEN** 서버는 200을 돌려주고 응답의 `nickname`은 "새닉네임"이다
- **AND** `name`과 `email`은 수정 전 값 그대로다

#### Scenario: 바꿀 수 없는 필드가 섞인 요청
- **WHEN** `{"nickname": "새닉", "loginId": "other01"}`을 보낸다
- **THEN** 서버는 200을 돌려주고 `nickname`만 바뀐다
- **AND** 응답의 `loginId`는 수정 전 값 그대로다

### Requirement: 수정 필드 규칙
서버는 보낸 필드의 앞뒤 공백을 지운 뒤 가입과 같은 규칙으로 검사해야 한다(SHALL). `name`은 1~20자, `nickname`은 1~10자, `email`은 이메일 형식이고 1~255자다. 하나라도 어기면 400 `COMMON-001`과 필드별 `errors`를 돌려주고 어떤 값도 바꾸지 않는다.

#### Scenario: 공백만 있는 닉네임
- **WHEN** `{"nickname": "   "}`을 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`에 `nickname` 항목이 들어 있다

#### Scenario: 앞뒤 공백이 있는 이름
- **WHEN** `{"name": " 홍길순 "}`을 보낸다
- **THEN** 서버는 200을 돌려주고 응답의 `name`은 "홍길순"이다

#### Scenario: 일부 필드만 틀린 요청
- **WHEN** `{"name": "새이름", "email": "hello@"}`을 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다
- **AND** 이후 내 정보를 조회하면 `name`은 수정 전 값 그대로다

### Requirement: 빈 수정 요청 거절
서버는 `name`, `nickname`, `email`이 모두 없거나 모두 `null`인 수정 요청에 400 `COMMON-001`을 돌려줘야 한다(SHALL).

#### Scenario: 빈 객체
- **WHEN** `PATCH /api/v1/users/me`에 `{}`를 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다

#### Scenario: 바꿀 수 없는 필드만 보냄
- **WHEN** `{"loginId": "other01"}`만 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다

### Requirement: 마이페이지 API의 사용자 확인
서버는 `/api/v1/users/me`와 그 아래 경로의 요청을 처리할 때, 토큰은 유효하지만 그 사용자가 없으면 401 `AUTH-003`을 돌려줘야 한다(SHALL). 토큰이 없거나 유효하지 않은 경우는 `api-common`의 JWT 인증 규칙을 따른다.

#### Scenario: 사용자가 사라진 토큰
- **WHEN** 로컬 서버를 다시 띄워 메모리 DB가 비워진 뒤, 재시작 전에 받은 토큰으로 `GET /api/v1/users/me`를 호출한다
- **THEN** 서버는 401 `AUTH-003`을 돌려준다

#### Scenario: 토큰 없음
- **WHEN** 토큰 없이 `GET /api/v1/users/me`를 호출한다
- **THEN** 서버는 401 `AUTH-003`을 돌려준다
