# Proposal

## Why

활동은 숨은그림 한 장을 받아야 시작되는데, `docs/API명세서.md` v0.2 5.1 `GET /pictures/next`가 서버에 아직 없다. 오류 코드 `PICTURE-001`(2.4)도 `ErrorCode`에 정의만 있고 쓰는 곳이 없다. 지금 프론트는 앱 안의 더미 그림으로 개발하고 있다. 서버가 그림과 찾을 대상의 위치를 내려줘야 그림을 앱 배포 없이 바꿀 수 있고, 6.2 기록 저장의 `pictureId`도 실제 값이 된다.

대응 절: 5.1 숨은그림 받기, 2.4 오류 코드 `PICTURE-001`, 2.5 공통 타입 `Mode`.

## What Changes

- `GET /api/v1/pictures/next?mode=COLOR|OBJECT&alertId=...`를 추가한다. 요청한 모드의 그림 중 한 장을 무작위로 골라 `pictureId`, `mode`, `imageUrl`, `aspectRatio`, `targets`를 돌려준다.
- `mode`가 없거나 잘못되면 400 `COMMON-001`이다. `alertId`는 선택 값이고 존재 여부를 확인하지 않는다.
- 요청한 모드의 그림이 없으면 404 `PICTURE-001`을 돌려준다.
- 그림 데이터는 DB 테이블 없이 `src/main/resources/pictures/catalog.json`에 두고, 서버가 기동할 때 읽어 메모리에 둔다.
- 기동할 때 그림 데이터를 검사한다. 대상이 1개 이상인지, `x`, `y`, `radius`가 0~1인지, `targetId`가 그림 안에서 고유한지 확인하고, 어기면 서버가 기동하지 않는다.
- 이미지 파일은 `src/main/resources/static/pictures/`에 두고 `/pictures/{파일 이름}`으로 토큰 없이 내려준다. `imageUrl`은 요청을 받은 서버 기준의 절대 URL이고, 설정 `pulse.public-base-url`이 있으면 그 값을 우선한다.
- 그림과 좌표가 확정되기 전에는 명세 5.1 예시(`pic_park_01`, 사물 모드, 고양이 4마리 좌표)로 데이터를 채워 개발을 진행한다.

## Capabilities

### New Capabilities

- `hidden-picture`: 모드별 숨은그림과 찾을 대상 좌표를 내려주는 동작, 그림 이미지 제공 경로, 그림 데이터의 형식 규칙

### Modified Capabilities

없음.

## Impact

- API: `GET /api/v1/pictures/next` 추가. 인증 없는 정적 경로 `/pictures/**`가 생긴다.
- 코드: `controller/PictureController`, `service/PictureService`, `repository/PictureCatalog`, `dto/picture/PictureResponse`, `dto/picture/TargetResponse`, `global/util/PublicUrlResolver` 추가. `entity/Mode`는 `add-record-mode-next-action`과 함께 쓰며, 먼저 구현하는 쪽이 만든다.
- 리소스: `src/main/resources/pictures/catalog.json`, `src/main/resources/static/pictures/`(이미지)
- 설정: `application.yml`에 `pulse.public-base-url`(환경변수 `PULSE_PUBLIC_BASE_URL`, 기본값 빈 문자열) 추가
- DB 변경 없음. 의존성 추가 없음. JSON 읽기는 이미 있는 Jackson을 쓴다.

## 범위 밖

- 그림을 올리거나 고치는 관리 API: 그림은 정적 데이터라 JSON과 이미지 파일을 바꿔 배포한다.
- 같은 사용자에게 같은 그림이 반복되지 않게 하는 선택: 명세는 "매번 달라져도 됩니다"라고만 정했다.
- 대상 영역을 `x, y, width, height`로 바꾸는 것: 명세 8.3에 "남음"으로 적힌 검토 과제다.
- 힌트 보기(Figma 06 최신안 15:11827의 전구 버튼): 앱이 `targets` 좌표로 처리할 수 있다.
- 현실 미션(Figma 08): 팀 결정(명세 8.1 1번)으로 제외했다.
- CDN이나 외부 저장소: 배포 환경이 정해지지 않았다(`docs/개발현황.md` 3.2).

## 미결 사항 (팀 확인 필요)

아래가 정해질 때까지는 명세 예시 데이터로 개발한다. 자세한 내용은 design.md의 Open Questions를 본다.

1. 실제 이미지 파일을 누가 주는지: Figma 프레임 `35:2869`의 공원 일러스트, 프론트 더미 `scene-park.jpg`(850×1850)
2. 고양이 개수와 좌표: 명세 예시는 4마리, 프론트 더미는 3마리다. Figma 안에서도 "1 / 3"(15:11827)과 "4 / 4"(15:11860)가 어긋난다.
3. 색 모드에서 무엇을 찾는지: 프론트 더미는 "보라색" 3개, Figma 옛 변형(9:3575)은 "파란색 3개"다. 정해지기 전에는 색 모드 그림이 없어 `mode=COLOR`가 `PICTURE-001`을 받는다.
4. 활동 한 번에 장면이 1장인지 2장인지: Figma 옛 변형에는 "장면 1 / 2", "장면 2 / 2"가 있다.
5. `emoji` 필드를 유지할지: Figma 최신안은 찾은 대상에 이모지 대신 원형 썸네일과 체크 표시를 쓴다.
