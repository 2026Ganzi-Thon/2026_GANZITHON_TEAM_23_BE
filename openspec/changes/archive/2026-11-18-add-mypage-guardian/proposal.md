# Proposal

## Why

`docs/API명세서.md` v0.2 7장은 마이페이지 API(7.1~7.4)와 보호자 1명 규칙을 정했지만(8.1 팀 결정 3번), 서버에는 아직 사용자 정보를 조회하거나 고치는 API가 없다. Figma 15번 마이페이지는 프로필 카드와 "정보 수정" 버튼, 보호자 연락처 메뉴를 보여 준다. Figma 12번 도움 요청의 "보호자에게 알리기"는 저장된 보호자 이름과 번호로 앱이 문자 앱을 연다. 프론트는 이미 7장 계약을 타입으로 반영했으므로 서버를 맞춘다.

대응 절: 7.1 내 정보 조회, 7.2 내 정보 수정, 7.3 보호자 등록·수정, 7.4 보호자 삭제, 2.5 공통 타입 `Guardian`.

## What Changes

- `GET /api/v1/users/me`를 추가한다. 가입 정보(`userId`, `name`, `nickname`, `email`, `loginId`)와 `guardian`을 돌려준다. 보호자가 없으면 `guardian`은 `null`이다.
- `PATCH /api/v1/users/me`를 추가한다. `name`, `nickname`, `email` 중 보낸 필드만 바꾼다. 규칙은 가입과 같고, 세 필드가 모두 없으면 400 `COMMON-001`이다. 응답은 7.1과 같은 형태다.
- `PUT /api/v1/users/me/guardian`을 추가한다. 보호자 이름과 휴대폰 번호를 등록하거나 덮어쓴다. 번호는 숫자만 남긴 뒤 검사하고 숫자만 저장한다.
- `DELETE /api/v1/users/me/guardian`을 추가한다. 보호자가 없어도 200이다.
- 보호자는 한 명이므로 별도 테이블 없이 `users` 테이블에 `guardian_name`, `guardian_phone` 열(NULL 허용)을 추가한다.
- 토큰은 유효하지만 사용자가 없으면 네 API 모두 401 `AUTH-003`을 돌려준다.

## Capabilities

### New Capabilities

- `user-profile`: 로그인한 사용자가 자기 프로필과 보호자를 조회하고, 이름·닉네임·이메일을 부분 수정하는 동작
- `guardian-contact`: 도움 요청에 쓸 보호자 한 명의 이름과 휴대폰 번호를 등록, 덮어쓰기, 삭제하는 동작과 번호 규칙

### Modified Capabilities

없음.

## Impact

- API: 위 네 엔드포인트 추가. 기존 API의 요청과 응답은 바뀌지 않는다. 가입·로그인 응답의 `user`에는 보호자를 넣지 않는다.
- 코드: `entity/User` 변경. `controller/UserController`, `service/UserService`, `dto/user/MeResponse`, `dto/user/UpdateMeRequest`, `dto/user/GuardianRequest`, `dto/user/GuardianResponse` 추가
- DB: `users` 테이블에 NULL 허용 열 두 개가 생긴다. 로컬은 `create-drop`이라 할 일이 없고, 운영의 `ddl-auto: update`로도 기존 행에 문제가 없다.
- 의존성 추가 없음. CORS 허용 메서드에 이미 PATCH, PUT, DELETE가 있다.

## 범위 밖

Figma에는 있지만 명세 v0.2에 API가 없어 팀 결정을 기다리는 기능이다. 이번 변경에서는 만들지 않는다. 근거 화면은 `docs/화면흐름.md`에 정리되어 있다.

- 회원 탈퇴: Figma 15번 마이페이지(15:12189) 하단의 "회원 탈퇴" 버튼. 탈퇴할 때 알림, 기록, 보호자를 어떻게 처리할지 정책이 먼저 필요하다.
- 이번 달 활동 수와 활동 후 불안 변화 평균: Figma 15번 마이페이지(15:12189)의 통계 두 칸. 앱이 6.1 목록으로 계산할지 새 API를 만들지 정해지지 않았다.
- 알림 기준값 조회와 "처음 기준으로 되돌리기": Figma 04번 설정(9:3342)의 알림 기준 영역. 서버는 사용자별 기준값을 갖고 있지만 조회와 초기화 API가 명세에 없다.

명세가 범위에서 뺀 것이다.

- 아이디와 비밀번호 변경(명세 7.2).
- 보호자 여러 명(명세 7장, 8.1 팀 결정 3번).
- 서버의 문자 발송. 앱이 휴대폰 문자 앱을 연다(명세 7장).
- 로그아웃 API. 명세에 없다.
