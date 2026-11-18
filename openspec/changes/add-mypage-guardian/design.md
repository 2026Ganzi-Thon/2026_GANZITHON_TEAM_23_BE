# Design

## Context

변경 이유는 proposal.md의 Why를 본다. 구현 전에 알아야 할 현재 상태는 다음과 같다.

- `entity/User`에는 `name`(20), `nickname`(10), `email`, `loginId`(20, 고유), `passwordHash`, `alertThreshold`, `createdAt`이 있고 모두 NOT NULL이다. 값을 바꾸는 메서드는 `raiseThreshold`뿐이다.
- 사용자 정보를 돌려주는 DTO는 `dto/auth/UserInfo`(가입·로그인 응답의 `user`)뿐이다.
- 가입 DTO `dto/auth/SignupRequest`는 compact constructor에서 앞뒤 공백을 지우고, Bean Validation 애너테이션으로 검사한다. 이번 변경도 같은 방식을 쓴다.
- 서버는 모르는 JSON 필드를 오류 없이 무시한다(2026-11-18 로컬 실행으로 확인). 그래서 PATCH 본문의 `loginId`, `password`는 따로 막지 않아도 무시된다.
- 토큰은 유효한데 사용자가 없으면 `service/AlertService.report`가 `BusinessException(ErrorCode.AUTH_003)`을 던진다. 이번 변경도 같은 방식을 쓴다.

## Goals / Non-Goals

**Goals:**

- 명세 7.1~7.4와 같은 요청·응답·오류를 만든다.
- 보호자 번호 검사 결과가 프론트와 같게 한다. 프론트는 숫자만 남긴 뒤 `^01\d{8,9}$`로 검사한다.

**Non-Goals:**

- 가입·로그인 응답(`dto/auth/UserInfo`)은 바꾸지 않는다.
- 이메일 중복 검사는 하지 않는다. 가입도 이메일 중복을 막지 않는다.

## Decisions

### 1. 보호자는 `users` 테이블의 열 두 개로 저장한다

`User`에 `guardianName`(`@Column(length = 20)`)과 `guardianPhone`(`@Column(length = 11)`)을 NULL 허용으로 추가한다. 보호자가 없으면 둘 다 `null`이다. 두 값은 `assignGuardian(name, phone)`과 `clearGuardian()`으로만 바꿔서, 한쪽만 `null`인 상태가 생기지 않게 한다.

- 검토한 대안: `guardians` 테이블을 따로 두고 사용자와 1:1 또는 1:N으로 연결. 팀 결정이 보호자 1명이고, 보호자를 참조하는 다른 기능이 없다. 조회마다 조인이나 추가 쿼리가 생기므로 고르지 않았다. 여러 명으로 바뀌면 그때 테이블로 옮긴다(Open Questions).

### 2. PATCH는 "`null`이면 바꾸지 않음"으로 해석한다

`UpdateMeRequest`는 `name`, `nickname`, `email`을 모두 NULL 허용으로 받는다. 서비스는 `null`이 아닌 필드만 `User.updateProfile(name, nickname, email)`에 넘긴다. 세 열이 모두 NOT NULL이라 "값을 지우는" 요청은 있을 수 없으므로, 필드가 없는 것과 `null`인 것을 구분할 필요가 없다.

- 검토한 대안: `Map`이나 `JsonNode`로 받아 "없음"과 "`null`"을 구분. 구분해도 쓸 곳이 없고 검증 애너테이션을 쓸 수 없어 고르지 않았다.

### 3. 입력 검증은 모두 DTO에서 끝낸다

Controller의 `@Valid`에서 검증이 끝나야 서비스가 값을 바꾸기 전에 실패한다. 그래서 일부 필드만 틀린 요청도 아무것도 바꾸지 않는다.

- `UpdateMeRequest`: compact constructor에서 세 필드의 앞뒤 공백을 지운다.
  - `name`: `@Size(min = 1, max = 20, message = "이름은 1~20자예요.")`
  - `nickname`: `@Size(min = 1, max = 10, message = "닉네임은 1~10자예요.")`
  - `email`: `@Email(message = "이메일 형식이 아닙니다.")`와 `@Size(min = 1, max = 255, message = "이메일은 1~255자예요.")`. `@Email`은 빈 문자열을 통과시키므로 `@Size(min = 1)`이 꼭 필요하다.
  - 세 필드가 모두 `null`인지: record에 `@AssertTrue(message = "바꿀 항목을 하나 이상 보내 주세요.") public boolean isAnyFieldPresent()`를 둔다. 이 메서드는 `is`로 시작하는 boolean 메서드라 검증기가 속성으로 인식한다. 실패하면 `errors`의 `field`는 `anyFieldPresent`가 된다.
  - 검토한 대안: 서비스에서 확인하고 `BusinessException(ErrorCode.COMMON_001)`을 던지기(가입의 비밀번호 바이트 검사와 같은 방식). 이유 문구가 `errors`에 남지 않아 고르지 않았다. 둘 다 400 `COMMON-001`이므로 계약에는 차이가 없다.
- `GuardianRequest`: compact constructor에서 `name`은 앞뒤 공백을 지우고, `phone`은 `phone.replaceAll("[^0-9]", "")`로 숫자만 남긴다(`null`이면 그대로 `null`).
  - `name`: `@NotBlank(message = "보호자 이름을 입력해 주세요.")`, `@Size(max = 20, message = "보호자 이름은 20자 이내예요.")`
  - `phone`: `@NotBlank(message = "보호자 번호를 입력해 주세요.")`, `@Pattern(regexp = "^01\\d{8,9}$", message = "휴대폰 번호를 확인해 주세요.")`
  - 검토한 대안: 원래 문자열을 하이픈을 허용하는 정규식으로 검사. 프론트 규칙과 결과가 달라질 수 있어 고르지 않았다.

### 4. 응답 DTO는 `dto/user`에 새로 만든다

`MeResponse(userId, name, nickname, email, loginId, guardian)`와 `GuardianResponse(name, phone)`를 만든다. `GuardianResponse.from(User)`는 보호자가 없으면 `null`을 돌려주고, `MeResponse.from(User)`가 그것을 쓴다. `guardian`이 `null`이어도 키는 응답에 들어간다(`api-common`의 빈 값 표기 규칙).

- 검토한 대안: `dto/auth/UserInfo`에 `guardian`을 더해 함께 쓰기. 가입·로그인 응답(3.1, 3.2)의 계약이 바뀌므로 고르지 않았다.

### 5. 사용자가 없으면 `AUTH-003`

`UserService`의 모든 메서드는 `users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_003))`로 시작한다. 로컬 서버를 다시 띄워 메모리 DB가 비워지면 예전 토큰이 이 경우에 해당한다. 앱은 `AUTH-003`을 받으면 다시 로그인한다(명세 2.1).

### 6. 경로와 트랜잭션

`UserController`는 `@RequestMapping("/api/v1/users/me")` 아래에 `GET`, `PATCH`, `PUT /guardian`, `DELETE /guardian`을 둔다. 조회는 `@Transactional(readOnly = true)`, 나머지는 `@Transactional`이고 변경은 엔티티 메서드와 변경 감지로 저장한다. 동시에 같은 사용자의 보호자를 바꾸면 마지막 요청이 남는다.

## 추가·변경 클래스

| 구분 | 경로 (`com.ganzithon.pulse` 아래) | 내용 |
|---|---|---|
| 변경 | `entity/User` | `guardianName`, `guardianPhone`(NULL 허용) 필드. `updateProfile(String name, String nickname, String email)`(`null`인 인자는 무시), `assignGuardian(String name, String phone)`, `clearGuardian()`, `getGuardianName()`, `getGuardianPhone()` |
| 추가 | `dto/user/MeResponse` | `record MeResponse(Long userId, String name, String nickname, String email, String loginId, GuardianResponse guardian)`, `static from(User)` |
| 추가 | `dto/user/GuardianResponse` | `record GuardianResponse(String name, String phone)`, `static from(User)`(보호자가 없으면 `null`) |
| 추가 | `dto/user/UpdateMeRequest` | `record UpdateMeRequest(String name, String nickname, String email)`, 공백 제거, 결정 3의 애너테이션, `isAnyFieldPresent()` |
| 추가 | `dto/user/GuardianRequest` | `record GuardianRequest(String name, String phone)`, 공백 제거와 숫자만 남기기, 결정 3의 애너테이션 |
| 추가 | `service/UserService` | `getMe(Long userId)`, `updateMe(Long userId, UpdateMeRequest)`, `putGuardian(Long userId, GuardianRequest)`, `deleteGuardian(Long userId)` |
| 추가 | `controller/UserController` | `/api/v1/users/me` 아래 네 엔드포인트. 조회·수정·보호자 등록은 `ApiResponse.ok(...)`, 삭제는 `ApiResponse.message("처리되었습니다.")`. 모두 200 |
| 변경 | `src/test/java/com/ganzithon/pulse/PulseFlowTest` | 마이페이지 시나리오 테스트 추가 |

## Risks / Trade-offs

- [Figma에는 정보 수정 화면과 보호자 입력 화면이 없다(`docs/화면흐름.md` 7장 "확인하지 못한 것"). 필드 구성은 명세만 보고 정했다.] → 화면이 나와서 다른 항목이 필요하면 명세 8.2의 "프로필 항목" 확인 절차를 따른다.
- [Figma 15번의 표시 이름 "하루"가 `name`인지 `nickname`인지 모른다.] → API는 둘 다 돌려주므로 서버는 영향이 없다. 프론트가 고른다.
- [번호에서 숫자가 아닌 문자를 모두 지우므로 "010-abcd-1234-5678" 같은 입력도 "01012345678"로 통과한다.] → 프론트와 같은 규칙이라 그대로 둔다. 오류가 나면 `errors[].value`에는 숫자만 남긴 값이 들어간다.
- [`@AssertTrue` 실패 시 `errors`의 `field`가 실제 필드가 아닌 `anyFieldPresent`다.] → 명세는 이 경우 `message`만 보여 준다고 했으므로 계약 문제는 아니다.
- [운영 DB에 NULL 허용 열 두 개가 추가된다.] → 기존 행은 보호자가 없는 상태가 되므로 문제가 없다.

## Migration Plan

- 로컬과 테스트: H2 메모리 DB에 `create-drop`이라 할 일이 없다.
- 운영: `ddl-auto: update`가 NULL 허용 열을 추가한다. 따로 할 일이 없다.
- 되돌리기: 코드를 되돌리면 API가 사라진다. 운영 DB에 남은 열은 쓰이지 않을 뿐 동작에 영향이 없다.

## Open Questions

- 보호자를 여러 명 두기로 팀 결정이 바뀌면 `guardians` 테이블로 옮기고 7장 계약도 바뀐다. 지금은 1명이다.
- 집 전화처럼 휴대폰이 아닌 번호를 받을지: 지금은 프론트 규칙대로 휴대폰 번호만 받는다. "보호자에게 문자" 용도라 휴대폰만으로 충분하다고 보지만 팀 확인이 필요하다.
