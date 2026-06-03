# Spotline API 명세서

Base URL: `https://{host}`

---

## 공통

- 날짜/시각 파라미터는 ISO 8601 형식 사용 (`2026-05-17T15:00:00`)
- 모든 응답은 `application/json` (스트리밍 제외)

---

## 1. 매장 (Store)

### GET /api/v1/store
매장 정보 조회

**Response 200**
```json
{
  "storeName": "string",
  "businessType": "string",
  "latitude": 0.0,
  "longitude": 0.0
}
```

---

### PUT /api/v1/store
매장 정보 등록 / 수정

**Request Body** `application/json`
```json
{
  "storeName": "string",
  "businessType": "string",
  "latitude": 0.0,
  "longitude": 0.0
}
```

**Response 200** — 위와 동일

---

### DELETE /api/v1/store
매장 정보 삭제

**Response 204** No Content

---

## 2. 영상 스트리밍 (Video)

### POST /api/v1/video/stream
프론트 → Spring Boot → 비전서버 영상 청크 중계

**Request Body** `multipart/form-data`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `fileChunk` | Binary | Y | 1~2초 영상 청크 |
| `createdAt` | String | Y | 촬영 시각 (ISO 8601, `2026-05-17T15:00:00`) |
| `sessionId` | String | N | 세션 식별자 (기본값: `"default"`) |

**Response 204** No Content

---

### GET /api/v1/video/stream
비전서버가 처리한 영상을 프론트로 스트리밍

**Response 200**
- Content-Type: `video/mp4`
- Body: mp4 청크 스트림 (chunked transfer)

---

## 3. 분석 데이터 수신 (Vision) — 비전서버 전용

### POST /api/v2/vision/data
비전서버 → Spring Boot 분석 데이터 전송

**Request Body** `multipart/form-data`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `data` | JSON | Y | 아래 구조 참고 |
| `video` | Binary | N | YOLO 처리된 mp4 청크 (있으면 프론트로 중계) |

**`data` JSON 구조**
```json
{
  "totalCount": 0,
  "maxResponseWaitTime": 0,
  "maxEmptyTableTime": 0,
  "coreCustomerAge": 20,
  "coreCustomerGender": 1,
  "avgDwellTime": 0,
  "justLeftCount": 0,
  "capturedAt": "2026-05-17T15:00:00",
  "endAt": "2026-05-17T15:00:00",
  "people": [
    {
      "id": 0,
      "age": 20,
      "gender": 1,
      "in": "2026-05-17T15:00:00",
      "out": "2026-05-17T15:00:00",
      "dwellTime": 0
    }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `coreCustomerAge` | 핵심 고객 나이대 (10, 20, 30, ...) |
| `coreCustomerGender` | 1 = 남성, 2 = 여성 |
| `avgDwellTime` | 평균 체류시간 (분) |
| `justLeftCount` | 그냥 나간 손님 수 |
| `people[].dwellTime` | 개인 체류시간 (초) |

**Response 204** No Content

---

## 4. 분석 API v1 (Analytics v1)

> `startAt`, `endAt` 쿼리 파라미터: ISO 8601 (`2026-05-17T15:00:00`)

### GET /api/v1/analytics/core-customers
핵심 고객 (성별 + 나이대)

**Query** `startAt`, `endAt`

**Response 200**
```json
{
  "gender": "string",
  "age": "string"
}
```

---

### GET /api/v1/analytics/hourly-population
나이대별 방문자 분포

**Query** `startAt`, `endAt`

**Response 200**
```json
{
  "age00s": 0,
  "age10s": 0,
  "age20s": 0,
  "age30s": 0,
  "age40s": 0,
  "age50s": 0
}
```

---

### GET /api/v1/analytics/weather-impact
날씨별 방문자 성과 분석

**Query** `startAt`, `endAt`

**Response 200**
```json
{
  "realValue": 0.0,
  "expectValue": 0.0,
  "adjustedValue": 0.0,
  "result": "GOOD | NORMAL | BAD"
}
```

---

### GET /api/v1/analytics/weekday-patterns
요일 패턴 성과 분석

**Query** `startAt`, `endAt`

**Response 200** — weather-impact와 동일 구조

---

### GET /api/v1/analytics/predictions/tomorrow
내일 예상 방문자 수 예측

**Response 200**
```json
{
  "expectedVisits": 0,
  "minVisits": 0,
  "maxVisits": 0
}
```

---

### GET /api/v1/analytics/predictions/next-week
다음 주 7일 예상 방문자 수 예측

**Response 200**
```json
{
  "result": [
    {
      "expectedVisits": 0,
      "minVisits": 0,
      "maxVisits": 0
    }
  ]
}
```

---

### GET /api/v1/analytics/daily-briefing
오늘 영업 일일 브리핑 (AI 생성 텍스트)

**Response 200**
```json
{ "message": "string" }
```

---

### GET /api/v1/analytics/marketing-recommendations
마케팅 추천 (AI 생성 텍스트)

**Response 200**
```json
{ "message": "string" }
```

---

### GET /api/v1/analytics/visits/daily
특정 날짜 총 방문자 수

**Query** `date` (ISO 8601 날짜, `2026-05-17`)

**Response 200**
```json
{ "totalVisits": 0 }
```

---

## 5. 분석 API v2 (Analytics v2)

> `startAt`, `endAt` 쿼리 파라미터: ISO 8601 (`2026-05-17T15:00:00`)

### GET /api/v2/analytics/current-count
현재 매장 내 인원 수

**Response 200**
```json
{ "count": 0 }
```

---

### GET /api/v2/analytics/peek-time
가장 바쁜 시간대 (시 단위)

**Query** `startAt`, `endAt`

**Response 200**
```json
{ "time": 0 }
```

---

### GET /api/v2/analytics/daily-sales
구간 내 총 매출 (원)

**Query** `startAt`, `endAt`

**Response 200**
```json
{ "dailySales": 0 }
```

---

### GET /api/v2/analytics/best-menu
가장 많이 팔린 메뉴

**Query** `startAt`, `endAt`

**Response 200**
```json
{ "menu": "string" }
```

---

### GET /api/v2/analytics/response-wait-time
최대 응대 대기 시간 (분)

**Query** `startAt`, `endAt`

**Response 200**
```json
{ "time": 0 }
```

---

### GET /api/v2/analytics/just-left-count
그냥 나간 손님 수

**Query** `startAt`, `endAt`

**Response 200**
```json
{ "count": 0 }
```

---

### GET /api/v2/analytics/empty-table-time
최대 테이블 유휴 시간 (분)

**Query** `startAt`, `endAt`

**Response 200**
```json
{ "time": 0 }
```

---

### GET /api/v2/analytics/daily-count
오늘 방문자 수 vs 평균 비교

**Query** `startAt`, `endAt`

**Response 200**
```json
{
  "count": 0,
  "avgCount": 0
}
```

---

### GET /api/v2/analytics/visit-trend
방문자 추세 (시계열)

**Query** `startAt`, `endAt`

**Response 200**
```json
{
  "time": ["2026-05-17T15:00:00"],
  "data": [0]
}
```

---

### GET /api/v2/analytics/core-customer
핵심 고객 나이대 및 성별

**Query** `startAt`, `endAt`

**Response 200**
```json
{
  "age": 20,
  "gender": 1
}
```

> `gender`: 1 = 남성, 2 = 여성

---

### GET /api/v2/analytics/avg-dwell
평균 체류시간 (분)

**Query** `startAt`, `endAt`

**Response 200**
```json
{ "time": 0 }
```

---

### GET /api/v2/analytics/gender-distribution
성별 방문자 분포

**Query** `startAt`, `endAt`

**Response 200**
```json
{
  "male": 0,
  "female": 0
}
```

> `-1` = 데이터 없음
