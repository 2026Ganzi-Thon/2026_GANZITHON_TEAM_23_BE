# Tasks

## 1. 사용자 엔티티

- [ ] 1.1 `entity/User`에 `guardianName`(`@Column(length = 20)`), `guardianPhone`(`@Column(length = 11)`)을 NULL 허용으로 추가하고, `updateProfile(name, nickname, email)`(`null` 인자는 무시), `assignGuardian(name, phone)`, `clearGuardian()`, 두 getter를 추가한다. 확인: `./gradlew compileJava` 성공, 기존 `./gradlew test` 통과

## 2. 내 정보 조회와 수정 (명세 7.1, 7.2)

- [ ] 2.1 `dto/user/GuardianResponse`, `dto/user/MeResponse`, `dto/user/UpdateMeRequest`를 design.md 결정 3, 4대로 만든다. 확인: `./gradlew compileJava` 성공
- [ ] 2.2 `service/UserService.getMe`, `updateMe`와 `controller/UserController`의 `GET /api/v1/users/me`, `PATCH /api/v1/users/me`를 구현한다. 사용자가 없으면 `AUTH_003`을 던진다. 확인: `./gradlew compileJava` 성공
- [ ] 2.3 테스트를 추가한다: 가입 직후 조회는 200이고 `"guardian": null` 키가 있음, `{"nickname":"새닉네임"}`은 닉네임만 바뀜, `{"name":" 홍길순 "}`은 "홍길순"으로 저장, `loginId`가 섞인 요청은 `loginId`를 바꾸지 않음, `{}`와 `{"loginId":"other01"}`은 400 `COMMON-001`, `{"nickname":"   "}`은 400과 `errors`의 `nickname` 항목, `{"name":"새이름","email":"hello@"}`은 400이고 이름이 바뀌지 않음, 토큰 없이 조회하면 401 `AUTH-003`, 테스트에서 `UserRepository`로 사용자를 지운 뒤 그 토큰으로 조회하면 401 `AUTH-003`. 확인: `./gradlew test` 통과
- [ ] 2.4 `README.md`의 API 구현 현황 표에서 7.1, 7.2 행의 상태를 `완료`로 바꾸고, 맨 위 "현재 상태" 문장의 개수를 맞춘다. 확인: 두 행의 상태가 `완료`이고 개수가 표와 일치함

## 3. 보호자 등록과 삭제 (명세 7.3, 7.4)

- [ ] 3.1 `dto/user/GuardianRequest`를 design.md 결정 3대로 만든다(이름 공백 제거, 번호는 숫자만 남김, `^01\d{8,9}$`). 확인: `./gradlew compileJava` 성공
- [ ] 3.2 `service/UserService.putGuardian`, `deleteGuardian`과 `controller/UserController`의 `PUT /api/v1/users/me/guardian`, `DELETE /api/v1/users/me/guardian`을 구현한다. 확인: `./gradlew compileJava` 성공
- [ ] 3.3 테스트를 추가한다: "010-1234-5678"은 200과 `phone` "01012345678"이고 내 정보 조회에 반영됨, 다시 PUT하면 덮어씀, "010 1234 5678"과 "011-123-4567"(결과 "0111234567")은 200, "02-123-4567"과 "+82 10-1234-5678"은 400과 `errors`의 `phone` 항목, 이름 누락과 21자 이름은 400, 보호자가 있을 때 빈 이름으로 PUT하면 400이고 기존 보호자가 그대로 남음, DELETE 후 조회하면 `guardian`이 `null`, 보호자가 없을 때 DELETE도 200과 `message` "처리되었습니다.". 확인: `./gradlew test` 통과
- [ ] 3.4 `README.md`의 API 구현 현황 표에서 7.3, 7.4 행의 상태를 `완료`로 바꾸고, 맨 위 "현재 상태" 문장의 개수를 맞춘다. 확인: 두 행의 상태가 `완료`이고 개수가 표와 일치함

## 4. 통합 확인

- [ ] 4.1 전체 테스트를 실행한다. 확인: `./gradlew test` 전체 통과
- [ ] 4.2 `./gradlew bootRun`으로 띄우고 가입, 내 정보 조회, 닉네임 수정, 보호자 등록, 조회, 보호자 삭제를 차례로 호출한다. `OPTIONS /api/v1/users/me`에 `Access-Control-Request-Method: PATCH`로 사전 요청도 보낸다. 확인: 응답이 명세 7.1~7.4 예시와 같은 필드를 갖고, 사전 요청 응답의 `Access-Control-Allow-Methods`에 PATCH가 있음
