# auth Specification

## Purpose

아이디와 비밀번호로 회원가입하고 로그인하는 규칙과, 이때 발급하는 액세스 토큰의 규칙을 정한다. 소셜 로그인은 없다. 계약 원문은 `docs/API명세서.md` 3장이다.

## Requirements

### Requirement: 회원가입
서버는 `POST /api/v1/auth/signup`으로 `name`, `nickname`, `email`, `loginId`, `password`를 받아 계정을 만들어야 한다(SHALL). 가입하면 바로 로그인한 것으로 보고 201과 함께 `accessToken`과 `user`를 돌려준다. `user`에는 `userId`, `name`, `nickname`, `email`, `loginId`가 들어 있다. 이 API는 토큰 없이 호출한다.

#### Scenario: 정상 가입
- **WHEN** 규칙에 맞는 값으로 가입을 요청한다
- **THEN** 서버는 201을 돌려준다
- **AND** `data.accessToken`은 빈 문자열이 아니고, `data.user`에 가입한 값과 새 `userId`가 들어 있다

### Requirement: 가입 입력값의 앞뒤 공백 제거
서버는 `name`, `nickname`, `email`, `loginId`의 앞뒤 공백을 지운 뒤 검증하고 저장해야 한다(SHALL). `password`는 공백을 지우지 않는다.

#### Scenario: 이름 앞뒤 공백
- **WHEN** `name`을 " 홍길동 "으로 보내 가입한다
- **THEN** 서버는 201을 돌려주고 응답의 `user.name`은 "홍길동"이다

#### Scenario: 공백만 있는 이름
- **WHEN** `name`을 공백 문자만으로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다
- **AND** `errors`에 `name` 항목(이유 "이름을 입력해 주세요.")이 들어 있다

### Requirement: 가입 필드 규칙
서버는 가입 값이 다음 규칙을 어기면 400 `COMMON-001`과 필드별 `errors`를 돌려줘야 한다(SHALL). 다섯 필드는 모두 필수다. `name`은 20자 이하, `nickname`은 10자 이하다. `email`은 이메일 형식이고 255자 이하다. `loginId`는 영문과 숫자만으로 4~20자다. `password`는 8자 이상 72자 이하다.

#### Scenario: 아이디 형식 위반
- **WHEN** `loginId`를 "x"로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다
- **AND** `errors`의 `loginId` 이유는 "아이디는 영문·숫자 4~20자예요."다

#### Scenario: 짧은 비밀번호
- **WHEN** `password`를 "1"로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다
- **AND** `errors`의 `password` 이유는 "비밀번호는 8자 이상 72자 이하예요."다

#### Scenario: 255자를 넘는 이메일
- **WHEN** 256자 이메일로 가입한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`에 `email` 항목이 들어 있다

### Requirement: 비밀번호 바이트 길이 제한
서버는 `password`를 UTF-8로 바꿨을 때 72바이트를 넘으면 글자 수가 72자 이하여도 가입을 거절해야 한다(SHALL). 비밀번호 해시가 72바이트까지만 받기 때문이다. 이때 400 `COMMON-001`을 돌려주고 `errors`는 빈 배열이다. 정확히 72바이트인 비밀번호는 가입과 로그인이 된다.

#### Scenario: 한글 30자 비밀번호
- **WHEN** `password`를 한글 30자(90바이트)로 보내 가입한다
- **THEN** 서버는 400 `COMMON-001`을 돌려주고 `errors`는 빈 배열이다

#### Scenario: 정확히 72바이트인 비밀번호
- **WHEN** 영문 72자 비밀번호로 가입하고 같은 비밀번호로 로그인한다
- **THEN** 가입은 201, 로그인은 200을 받는다

### Requirement: 아이디 중복 거절
서버는 이미 있는 `loginId`로 가입하면 409 `AUTH-002`와 메시지 "이미 사용 중인 아이디예요."를 돌려줘야 한다(SHALL). 같은 아이디로 동시에 가입해 저장 단계에서 충돌해도 같은 오류를 돌려준다.

#### Scenario: 같은 아이디로 두 번 가입
- **WHEN** 같은 `loginId`로 두 번 가입한다
- **THEN** 두 번째 요청은 409 `AUTH-002`를 받는다

### Requirement: 비밀번호 저장 방식
서버는 비밀번호 원문을 저장하면 안 되며(MUST NOT), 단방향 해시 값만 저장해야 한다(MUST).

#### Scenario: 저장된 비밀번호 확인
- **WHEN** 가입한 뒤 로컬 H2 콘솔에서 `users` 테이블을 조회한다
- **THEN** `password_hash` 칸에는 가입 때 보낸 원문이 아닌 해시 값이 들어 있다

### Requirement: 로그인
서버는 `POST /api/v1/auth/login`으로 `loginId`와 `password`를 받아 일치하면 200과 함께 가입 응답과 같은 형태(`accessToken`, `user`)를 돌려줘야 한다(SHALL). 두 값은 필수이고 비어 있으면 400 `COMMON-001`을 돌려준다. 이 API는 토큰 없이 호출한다.

#### Scenario: 로그인 성공
- **WHEN** 가입한 아이디와 비밀번호로 로그인한다
- **THEN** 서버는 200과 새 `accessToken`, `user`를 돌려준다

#### Scenario: 빈 값으로 로그인
- **WHEN** `loginId`와 `password`를 빈 문자열로 보낸다
- **THEN** 서버는 400 `COMMON-001`을 돌려준다
- **AND** `errors`에 이유 "아이디를 입력해 주세요."와 "비밀번호를 입력해 주세요."가 들어 있다

### Requirement: 로그인 실패
서버는 아이디가 없거나 비밀번호가 틀리면 어느 쪽이 틀렸는지 구분하지 않고 401 `AUTH-001`과 메시지 "입력 내용을 확인해 주세요."를 돌려줘야 한다(SHALL). UTF-8로 72바이트를 넘는 비밀번호도 서버 오류 없이 `AUTH-001`로 처리한다.

#### Scenario: 틀린 비밀번호
- **WHEN** 가입한 아이디에 다른 비밀번호로 로그인한다
- **THEN** 서버는 401 `AUTH-001`을 돌려준다

#### Scenario: 72바이트를 넘는 비밀번호
- **WHEN** 영문 100자 비밀번호로 로그인한다
- **THEN** 서버는 500이 아닌 401 `AUTH-001`을 돌려준다

#### Scenario: 없는 아이디
- **WHEN** 가입하지 않은 아이디로 한글 30자 비밀번호를 보내 로그인한다
- **THEN** 서버는 401 `AUTH-001`을 돌려준다

### Requirement: 액세스 토큰
서버는 사용자 ID를 담아 서명한 JWT를 액세스 토큰으로 발급해야 한다(SHALL). 토큰은 발급 후 24시간 동안 유효하며 설정 `pulse.jwt.expiration-minutes`(기본 1440분)로 정한다. 갱신(refresh) 토큰은 없다. 토큰이 만료되면 클라이언트는 `AUTH-003`을 받고 다시 로그인해 새 토큰을 받는다.

#### Scenario: 만료된 토큰
- **WHEN** 발급 후 24시간이 지난 토큰으로 인증이 필요한 API를 호출한다
- **THEN** 서버는 401 `AUTH-003`을 돌려준다

#### Scenario: 다시 로그인
- **WHEN** 토큰이 만료된 사용자가 로그인 API를 호출한다
- **THEN** 서버는 200과 새 토큰을 돌려준다

### Requirement: 토큰 서명 키
서버는 운영(`prod`) 프로필에서 토큰 서명 키를 환경변수 `JWT_SECRET`으로만 받아야 한다(MUST). 값이 없으면 서버는 기동하지 않는다. 로컬과 테스트는 개발용 기본 키를 쓴다.

#### Scenario: 운영 프로필에 서명 키가 없음
- **WHEN** `JWT_SECRET` 없이 `prod` 프로필로 서버를 띄운다
- **THEN** 서버는 기동에 실패한다
