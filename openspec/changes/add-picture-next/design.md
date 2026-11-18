# Design

## Context

변경 이유는 proposal.md의 Why를 본다. 구현 전에 알아야 할 현재 상태는 다음과 같다.

- `global/exception/ErrorCode.PICTURE_001`(404, "그림을 불러오지 못했어요.")이 이미 있다. 새로 만들지 않고 쓴다.
- Spring Boot는 기본 설정으로 `classpath:/static/` 아래 파일을 `/**` 경로로 내려준다. 레포에는 아직 `src/main/resources/static/`이 없다.
- 인증 인터셉터는 `/api/v1/**`에만 걸리고(`global/config/WebConfig`), CORS 매핑은 `/api/**`에만 있다. 따라서 `/pictures/**`는 인증하지 않는다.
- 없는 정적 파일을 요청하면 기존 예외 처리기가 404 `COMMON-001`을 돌려준다(2026-11-18 로컬 실행에서 `/pictures/none.png`로 확인).
- 필수 쿼리 파라미터가 없으면 기존 예외 처리기가 400 `COMMON-001`과 빈 `errors`를, 쿼리 값의 타입이 맞지 않으면 400 `COMMON-001`과 `errors[0].field`를 돌려준다. 예외 처리기를 고칠 필요는 없다.
- 그림 이미지 파일과 확정된 좌표는 아직 없다. 미결 사항은 아래 Open Questions에 있다.

## Goals / Non-Goals

**Goals:**

- 명세 5.1과 같은 요청·응답·오류를 만든다.
- 그림과 좌표가 확정되기 전에도 명세 예시 데이터로 프론트와 연동할 수 있게 한다.
- 그림을 바꿀 때 Java 코드를 고치지 않고 JSON과 이미지 파일만 바꾸게 한다.

**Non-Goals:**

- 그림 데이터를 DB에 두지 않는다.
- 이미지 파일이 실제로 있는지는 기동할 때 검사하지 않는다. 이미지가 확정되기 전에도 서버가 떠야 하기 때문이다.

## Decisions

### 1. 그림 데이터는 JSON 카탈로그로 두고 기동할 때 메모리에 올린다

그림은 사용자가 만들거나 바꾸지 않는 정적 데이터다. `src/main/resources/pictures/catalog.json`에 두고, 서버가 기동할 때 한 번 읽어 모드별 목록으로 메모리에 둔다.

- 검토한 대안: JPA 엔티티(`Picture`, `PictureTarget`)와 시드 데이터. 로컬 DB가 `create-drop`이라 기동할 때마다 `data.sql`이나 초기화 코드로 다시 넣어야 하고, 운영(`ddl-auto: update`)에서는 중복 삽입을 막는 처리가 따로 필요하다. 그림을 관리하는 API도 없어서 DB에 둘 이유가 없으므로 고르지 않았다.
- 검토한 대안: Java 코드에 상수로 두기. 좌표를 조정할 때마다 코드를 고쳐야 하고, 개발자가 아닌 팀원이 값을 보기 어려워 고르지 않았다.

### 2. 카탈로그 형식

최상위는 그림 객체의 배열이다. `image`는 `src/main/resources/static/pictures/` 안의 파일 이름이다. 아래는 그림이 확정되기 전에 쓸 초기 데이터이며 명세 5.1 예시와 값이 같다.

```json
[
  {
    "pictureId": "pic_park_01",
    "mode": "OBJECT",
    "image": "park_01.png",
    "aspectRatio": 0.4595,
    "targets": [
      { "targetId": "cat-tree", "label": "고양이", "emoji": "🐱", "x": 0.159, "y": 0.174, "radius": 0.088 },
      { "targetId": "cat-rock", "label": "고양이", "emoji": "🐱", "x": 0.618, "y": 0.520, "radius": 0.065 },
      { "targetId": "cat-bush", "label": "고양이", "emoji": "🐱", "x": 0.926, "y": 0.711, "radius": 0.076 },
      { "targetId": "cat-sleep", "label": "고양이", "emoji": "🐱", "x": 0.812, "y": 0.859, "radius": 0.112 }
    ]
  }
]
```

- 이 데이터에는 색 모드 그림이 없다. 그래서 확정 전에는 `mode=COLOR`가 404 `PICTURE-001`을 받는다(Risks 참고).
- 검토한 대안: `{ "pictures": [...] }`처럼 객체로 감싸기. 지금은 그림 목록 말고 담을 정보가 없어 배열로 두었다.

### 3. 카탈로그 클래스는 `repository/`에 두고 데이터 모델은 그 안의 중첩 record로 둔다

`repository/PictureCatalog`(`@Component`)가 카탈로그를 읽고 검사하고 모드별로 돌려준다. 데이터를 읽어 오는 역할이 Repository와 같으므로 `repository/`에 둔다. 그림과 대상은 `PictureCatalog.Picture`, `PictureCatalog.Target` 중첩 record로 둔다.

- 검토한 대안: `entity/Picture` record. `entity/`는 JPA 엔티티를 두는 곳이라 `@Entity`가 아닌 클래스를 두면 헷갈리므로 고르지 않았다.
- 응답 DTO는 계층 규칙대로 `dto/picture/PictureResponse`, `dto/picture/TargetResponse`로 따로 만든다.

### 4. 읽기와 검사

- Spring이 쓰는 생성자(`@Autowired`)는 `ObjectMapper`로 `classpath:pictures/catalog.json`을 읽은 뒤, 목록을 받는 다른 생성자에 넘긴다. 목록을 받는 생성자는 테스트에서 Spring 없이 쓴다.
- 검사는 `static void validate(List<Picture>)`로 둔다. 규칙은 `hidden-picture` 스펙의 "그림 데이터 검증"과 같다. `mode` 값이 enum에 없으면 JSON을 읽는 단계에서 실패한다.
- 규칙을 어기면 `IllegalStateException`을 던진다. 메시지에는 `pictureId`, 해당하면 `targetId`, 어긴 규칙을 적는다(예: `pic_park_01 / cat-tree: x는 0~1이어야 합니다 (1.2)`). 예외가 빈 생성 중에 나므로 애플리케이션이 기동하지 않고, `./gradlew test`도 컨텍스트 로딩에서 실패한다. 그래서 잘못된 데이터가 배포 전에 걸린다.
- 읽은 결과는 `Map<Mode, List<Picture>>`로 바꿔 변경할 수 없게 보관한다.
- 검사 규칙: `pictureId` 비어 있지 않고 전체에서 고유, `mode` 있음, `image` 비어 있지 않음, `aspectRatio` 0 초과, `targets` 1개 이상, 대상마다 `targetId` 비어 있지 않고 그림 안에서 고유, `x`·`y` 0 이상 1 이하, `radius` 0 초과 1 이하, `label`·`emoji` 비어 있지 않음. `radius`는 0을 허용하지 않는다. Jackson은 JSON에서 빠진 `double` 키를 0.0으로 채우므로, 0을 허용하면 `radius` 키를 빠뜨려도 기동이 성공하기 때문이다. `label`·`emoji`는 응답에 그대로 나가는 필수 필드라 빈 문자열을 막는다. 앞의 대상 좌표 검사에 더해 `pictureId` 전체 고유와 `aspectRatio` 0 초과도 검사한다. 앞의 것은 6.2 기록의 `pictureId`가 어느 그림인지 모호해지는 것을 막고, 뒤의 것은 앱의 화면 비율 계산이 깨지는 것을 막는다.
- 검토한 대안: 요청을 받을 때마다 검사. 잘못된 데이터를 사용자가 먼저 만나게 되므로 고르지 않았다.

### 5. 그림 선택은 `service/PictureService`가 무작위로 한다

`PictureService.next(Mode mode)`가 카탈로그에서 그 모드의 목록을 받아 `ThreadLocalRandom.current().nextInt(size)`로 한 장을 고른다. 목록이 비었으면 `BusinessException(ErrorCode.PICTURE_001)`을 던진다. 사용자별 이력은 보지 않는다.

- 테스트는 모드마다 그림이 한 장인 목록을 쓰므로 무작위성의 영향을 받지 않는다.

### 6. `imageUrl`은 `global/util/PublicUrlResolver`가 만든다

`PublicUrlResolver`(`@Component`)는 설정 `pulse.public-base-url`을 읽는다. 값이 있으면 끝의 `/`를 지우고 `/pictures/{image}`를 붙인다. 값이 없으면 `ServletUriComponentsBuilder.fromCurrentContextPath()`로 요청을 받은 서버의 스킴, 호스트, 포트를 쓴다. `application.yml`에는 `pulse.public-base-url: ${PULSE_PUBLIC_BASE_URL:}`을 추가한다.

- 리버스 프록시 뒤에 배포하면 요청 기준 주소가 내부 주소나 `http://` 주소가 될 수 있다. 그래서 `prod` 프로필은 `PULSE_PUBLIC_BASE_URL`을 필수로 두고(`application-prod.yml`, 기본값 없음, `JWT_SECRET`과 같은 방식), 기본 프로필(로컬·테스트)만 요청 주소를 쓴다.
- 검토한 대안: `server.forward-headers-strategy=framework`로 프록시의 `X-Forwarded-*` 헤더를 믿기. 배포 환경과 프록시 설정이 정해지지 않아(`docs/개발현황.md` 3.2) 지금은 고르지 않았다. 나중에 함께 쓸 수 있다.
- 검토한 대안: `/pictures/park_01.png` 같은 상대 경로. 명세 5.1의 `imageUrl`은 URL이고, React Native의 `Image`는 절대 URL이 필요하므로 고르지 않았다.

### 7. 이미지는 Spring Boot 기본 정적 리소스 처리로 내려준다

이미지 파일을 `src/main/resources/static/pictures/`에 두면 `/pictures/{파일 이름}`으로 나간다. `WebConfig`를 고치지 않는다. 인증 인터셉터 대상(`/api/v1/**`)이 아니므로 토큰 없이 받을 수 있다. 이미지는 민감한 정보가 아니라 공개해도 된다.

- 검토한 대안: API(`/api/v1/pictures/{id}/image`)로 내려주기. 앱의 `Image` 컴포넌트에 인증 헤더를 붙여야 하고 코드가 늘어서 고르지 않았다.

### 8. 쿼리 파라미터 바인딩

`PictureController`는 `@RequestParam Mode mode`와 `@RequestParam(required = false) String alertId`를 받는다. `mode`의 누락과 잘못된 값은 기존 예외 처리기가 400 `COMMON-001`로 바꾼다. `alertId`는 계약을 드러내려고 선언만 하고 지금은 쓰지 않는다.

## 추가·변경 클래스

| 구분 | 경로 (`com.ganzithon.pulse` 아래) | 내용 |
|---|---|---|
| 추가 | `entity/Mode` | enum `COLOR`, `OBJECT`. `add-record-mode-next-action`이 먼저 만들었으면 그것을 쓴다 |
| 추가 | `repository/PictureCatalog` | `@Component`. 중첩 record `Picture(String pictureId, Mode mode, String image, double aspectRatio, List<Target> targets)`, `Target(String targetId, String label, String emoji, double x, double y, double radius)`. `findByMode(Mode)`, `static validate(List<Picture>)` |
| 추가 | `service/PictureService` | `PictureResponse next(Mode mode)`. 무작위 선택, 없으면 `PICTURE_001` |
| 추가 | `controller/PictureController` | `GET /api/v1/pictures/next`, `ApiResponse.ok(...)`, 200 |
| 추가 | `dto/picture/PictureResponse` | `record PictureResponse(String pictureId, Mode mode, String imageUrl, double aspectRatio, List<TargetResponse> targets)` |
| 추가 | `dto/picture/TargetResponse` | `record TargetResponse(String targetId, String label, String emoji, double x, double y, double radius)` |
| 추가 | `global/util/PublicUrlResolver` | `@Component`. `String pictureUrl(String image)` |
| 추가 | `src/main/resources/pictures/catalog.json` | 결정 2의 초기 데이터 |
| 추가 | `src/main/resources/static/pictures/` | 확정된 이미지 파일 |
| 변경 | `src/main/resources/application.yml` | `pulse.public-base-url: ${PULSE_PUBLIC_BASE_URL:}` |
| 변경 | `src/main/resources/application-prod.yml` | `pulse.public-base-url: ${PULSE_PUBLIC_BASE_URL}` (기본값 없음) |
| 추가 | `src/test/java/com/ganzithon/pulse/repository/PictureCatalogTest` | 검사 규칙 단위 테스트(Spring 없이) |
| 추가 | `src/test/java/com/ganzithon/pulse/service/PictureServiceTest` | 그림 없음(`PICTURE_001`), 모드 필터, 기준 주소 설정 단위 테스트 |
| 추가 | `src/test/java/com/ganzithon/pulse/controller/PictureControllerTest` | 빈 카탈로그에서 404 `PICTURE-001` HTTP 응답(Spring 컨텍스트 없이 standalone MockMvc) |
| 변경 | `src/test/java/com/ganzithon/pulse/PulseFlowTest` | API 통합 테스트 |
| 추가 | `src/test/resources/static/pictures/` | 정적 경로 확인용 작은 테스트 파일 |

## Risks / Trade-offs

- [이미지 파일이 오기 전에는 `imageUrl`이 404를 받는다.] → 미결 사항 1이 정해질 때까지 프론트는 앱 안의 더미 이미지를 함께 쓴다. API 연동(좌표, 개수, 기록 저장의 `pictureId`)은 먼저 확인할 수 있다.
- [확정 전에는 색 모드 그림이 없어 `mode=COLOR`가 `PICTURE-001`을 받고, "다른 모드" 흐름이 막힌다.] → 프론트는 명세대로 "그림을 불러오지 못했어요."와 다시 불러오기를 보여 준다. 미결 사항 3이 정해지면 카탈로그에 색 모드 그림을 넣는다.
- [좌표가 실제 이미지와 맞지 않을 수 있다. 명세 예시는 850×1850 기준이지만 Figma 장면 프레임은 402×802(비율 약 0.501)이고, `docs/화면흐름.md`에 따르면 Figma에서 눈으로 잰 y값은 예시보다 0.03~0.09 낮다.] → 좌표는 확정된 이미지 파일 위에서 다시 잰다. 프론트는 이미지를 `aspectRatio`대로 자르지 않고 그려야 비율 좌표가 맞는다. 이 표시 방식을 프론트와 맞춘다.
- [운영을 프록시 뒤에 두고 `PULSE_PUBLIC_BASE_URL`을 빠뜨리면 `Host` 헤더가 그대로 `imageUrl`에 들어가 `http://` 주소가 나가고 iOS가 이미지를 막는다.] → `application-prod.yml`이 `pulse.public-base-url: ${PULSE_PUBLIC_BASE_URL}`을 기본값 없이 요구해, `prod`에서 변수가 없으면 서버가 기동하지 않는다. 기본 프로필은 그대로 빈 값을 허용한다.
- [카탈로그 오류는 서버 기동 자체를 막는다.] → 의도한 동작이다. `./gradlew test`가 실제 카탈로그로 컨텍스트를 띄우므로 배포 전에 걸린다.
- [`/pictures/**`는 로그인 없이 누구나 받을 수 있다.] → 일러스트라 공개해도 된다.

## Migration Plan

- DB 변경이 없다.
- 운영: `prod` 프로필은 `PULSE_PUBLIC_BASE_URL`을 필수로 요구한다(`application-prod.yml`에 기본값이 없어 변수가 없으면 기동이 실패한다).
- 되돌리기: 코드와 리소스를 되돌리면 된다.

## Open Questions

모두 팀 확인이 필요하다. 1, 2, 3, 5는 카탈로그 데이터만 바꾸면 되므로 확정 전에도 개발을 진행할 수 있다. 4는 결과에 따라 스펙이 바뀔 수 있다.

1. 실제 이미지 파일을 누가 주는가: Figma 프레임 `35:2869`(402×802 프레임)에 공원 일러스트가 있고, 프론트 더미는 `scene-park.jpg`(850×1850)다. 원본 해상도와 파일 형식(PNG 또는 JPG)이 정해지면 `aspectRatio`와 `image` 값을 그에 맞춘다.
2. 고양이 개수와 좌표: 명세 5.1 예시는 4마리, 프론트 더미는 3마리다. Figma 안에서도 최신 장면 화면은 "1 / 3"(15:11827)이고 완료 화면은 "4 / 4"(15:11860, "고양이 4마리를 모두 찾았어요")다. 개수를 정한 뒤 확정 이미지 위에서 좌표와 반경을 잰다.
3. 색 모드에서 무엇을 찾는가: 프론트 더미는 "보라색" 3개, Figma 옛 변형(9:3575)은 "파란색을 3개 찾아보세요"다. 명세 8.2는 `label`을 색 이름으로 가정했다. 색 모드용 그림을 따로 둘지, 공원 그림을 함께 쓸지도 정해야 한다.
4. 활동 한 번에 장면이 1장인가 2장인가: Figma 옛 변형(9:3575, 9:3623)에는 "장면 1 / 2", "장면 2 / 2"가 있다. 2장이면 5.1을 두 번 부르게 되고 6.2 기록의 `pictureId`가 하나뿐이라 계약을 다시 정해야 한다. 지금 설계는 1장 기준이다.
5. `emoji`를 유지하는가: Figma 최신안(15:11827, 15:11860)은 찾은 대상에 이모지 대신 원형 썸네일과 체크 표시를 쓴다. 지금은 명세 v0.2대로 `emoji`를 내려준다. 빼려면 명세와 프론트 타입을 함께 바꾼다.
