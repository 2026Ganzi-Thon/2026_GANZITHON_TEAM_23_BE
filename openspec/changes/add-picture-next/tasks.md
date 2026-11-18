# Tasks

## 1. 이미지와 좌표 확정 (팀 확인)

- [ ] 1.1 design.md Open Questions 1~5(이미지 제공자와 파일, 고양이 개수와 좌표, 색 모드 대상, 장면 수, `emoji` 유지)를 팀과 확정한다. 이 작업은 개발을 막지 않는다. 확정 전에는 2장부터 명세 5.1 예시 데이터로 진행한다. 확인: design.md Open Questions의 각 항목에 결정 결과가 적혀 있음

## 2. 그림 데이터와 검증

- [ ] 2.1 `entity/Mode`(`COLOR`, `OBJECT`)가 없으면 만든다(`add-record-mode-next-action`과 함께 씀). 확인: `./gradlew compileJava` 성공
- [ ] 2.2 `src/main/resources/pictures/catalog.json`을 design.md 결정 2의 초기 데이터(`pic_park_01`, `OBJECT`, `park_01.png`, 0.4595, 고양이 4마리)로 만든다. 확인: 파일의 좌표가 명세 5.1 예시와 같음
- [ ] 2.3 `repository/PictureCatalog`를 design.md 결정 3, 4대로 구현한다(읽기, `validate`, 모드별 보관, 테스트용 목록 생성자). 확인: `./gradlew compileJava` 성공
- [ ] 2.4 `PictureCatalogTest`(Spring 없이)를 추가한다: 정상 데이터 통과, 빈 `targets`, `x` 1.2, `y` -0.1, `radius` 1.5, 그림 안의 같은 `targetId`, 전체에서 같은 `pictureId`, `aspectRatio` 0이 각각 `IllegalStateException`이고 메시지에 `pictureId`가 들어 있음, 실제 `catalog.json`을 읽어 검사를 통과함. 확인: `./gradlew test` 통과

## 3. 숨은그림 받기 API (명세 5.1)

- [ ] 3.1 `global/util/PublicUrlResolver`를 만들고 `application.yml`에 `pulse.public-base-url: ${PULSE_PUBLIC_BASE_URL:}`을 추가한다. 확인: `./gradlew compileJava` 성공
- [ ] 3.2 `dto/picture/PictureResponse`, `dto/picture/TargetResponse`, `service/PictureService.next(Mode)`(무작위 선택, 없으면 `PICTURE_001`), `controller/PictureController`의 `GET /api/v1/pictures/next`를 구현한다. 확인: `./gradlew compileJava` 성공
- [ ] 3.3 `PictureServiceTest`(Spring 없이)를 추가한다: 색 모드 그림이 없는 목록에서 `COLOR`를 요청하면 `ErrorCode.PICTURE_001`의 `BusinessException`, 사물·색 그림이 섞인 목록에서 `COLOR`를 요청하면 항상 `mode`가 `COLOR`, 기준 주소를 `https://api.example.com/`로 주면 `imageUrl`이 `https://api.example.com/pictures/park_01.png`. 확인: `./gradlew test` 통과
- [ ] 3.4 `PulseFlowTest`에 통합 테스트를 추가한다: `mode=OBJECT`는 200이고 `data.mode` "OBJECT", `targets` 1개 이상, 각 항목에 여섯 필드, `imageUrl`이 `http://localhost/pictures/`로 시작함, `alertId=alert_00000000_9999`를 붙여도 200, `mode` 누락은 400 `COMMON-001`, `mode=PURPLE`은 400과 `errors[0].field` "mode", 토큰 없이 호출하면 401 `AUTH-003`. 확인: `./gradlew test` 통과
- [ ] 3.5 `README.md`의 API 구현 현황 표에서 5.1 행의 상태를 `완료 (그림 데이터는 확정 전 명세 예시)`로 바꾸고, 맨 위 "현재 상태" 문장의 개수를 맞춘다. 아래 작업 5.1에서 확정 데이터를 반영하면 괄호를 지운다. 확인: 5.1 행의 상태가 바뀌고 개수가 표와 일치함

## 4. 이미지 제공

- [ ] 4.1 `src/test/resources/static/pictures/`에 작은 테스트 파일을 두고, 토큰 없이 `GET /pictures/<그 파일>`이 200이고 `GET /pictures/none.png`가 404 `COMMON-001`인지 확인하는 테스트를 추가한다. 확인: `./gradlew test` 통과
- [ ] 4.2 1.1에서 확정한 이미지 파일을 `src/main/resources/static/pictures/`에 넣고 `catalog.json`의 `image` 값을 파일 이름에 맞춘다. 확인: `./gradlew bootRun` 후 `GET /api/v1/pictures/next?mode=OBJECT` 응답의 `imageUrl`을 열면 200과 이미지가 나옴

## 5. 확정값 반영

- [ ] 5.1 1.1 결과로 `catalog.json`을 갱신한다(고양이 개수와 좌표, `aspectRatio`, 색 모드 그림, `emoji`). 확인: `PictureCatalogTest`의 실제 카탈로그 검사가 통과하고, 프론트 앱에서 각 대상을 눌렀을 때 정답으로 인정되는지 함께 확인함
- [ ] 5.2 1.1에서 장면이 2장으로 정해지거나 `emoji`를 빼기로 하면 이 변경의 스펙과 design.md를 먼저 고친다. 해당하지 않으면 그 사실을 design.md Open Questions에 적는다. 확인: `openspec validate add-picture-next --strict` 통과

## 6. 통합 확인

- [ ] 6.1 전체 테스트를 실행한다. 확인: `./gradlew test` 전체 통과
- [ ] 6.2 `./gradlew bootRun`으로 띄우고 가입, `GET /api/v1/pictures/next?mode=OBJECT`, 받은 `pictureId`로 기록 저장(6.2)을 차례로 호출한다. 확인: 응답이 명세 5.1 예시와 같은 필드를 갖고 기록 목록에 그 `pictureId`가 보임
