# Spotline

**CCTV를 통한 오프라인 로그 수집 솔루션** — 매장이 스스로 말하게 한다

## 배경

온라인 사업자는 고객의 클릭, 체류, 이탈을 모두 데이터로 본다. 반면 오프라인 점포 운영자는 오늘 몇 명이 방문했는지조차 정확히 모른 채, 여전히 직감에 의존해 핵심 경영 판단을 내리고 있다.

Spotline은 기존 CCTV·웹캠·휴대폰 카메라를 별도 장비 투자 없이 "오프라인 사용자 로그 수집 도구"로 확장하여, Vision AI로 매장 내 고객의 방문·행동 데이터를 수집·분석하고 데이터 기반 운영 전략 수립을 지원하는 플랫폼이다.

## 주요 기능

- **방문/행동 데이터 수집**: 출입 인원 탐지 및 동선 추적, 성별/연령대 등 고객 속성 추정 (비식별 형태)
- **실시간 운영 대시보드**: 방문 인원 수, 시간대별 방문 빈도, 혼잡도, 체류 시간 시각화
- **핵심 고객 분석**: 매출 기여도가 높은 핵심 고객 세그먼트 식별
- **날씨 보정 성과 분석**: 기상·요일 변수를 제거한 매장 고유 성과(Weather-Adjusted Performance) 산출
- **이상 탐지 & 추세 분석**: 요일별 정상 범위(Z-score) 이탈 탐지, 단·중·장기 이동평균 추세선
- **단기 방문 예측**: 내일/다음 주 예상 방문자 수 및 신뢰구간 예측
- **AI 일일 브리핑 & 마케팅 제안**: LLM(Vertex AI/Gemini) 기반 자연어 인사이트 및 운영 개선안 자동 생성
- **챗봇 인터페이스**: 대시보드 분석 없이 자연어 질문으로 즉시 인사이트 조회
- **POS 연동**: Toss POS 연동을 통한 매출 데이터 결합 분석

## 아키텍처

```
클라이언트 웹 사이트
      │ HTTPS
      ▼
   NGINX (리버스 프록시)
      │
      ├── React (프론트엔드)
      └── Spring Boot (백엔드, 본 저장소)
             │
             ├── FastAPI → YOLOv10/BoT-SORT (객체 추출 AI)
             ├── Vertex AI / Gemini (일일 브리핑 및 마케팅 제안)
             ├── Toss POS API (매출 조회)
             ├── Open-Meteo API (기상 데이터)
             └── Google Cloud (Cloud Storage / Pub/Sub / Cloud Tasks / Cloud SQL / Cloud Logging)
```

본 저장소는 위 아키텍처 중 **Spring Boot 백엔드**를 담당하며, 영상 스트리밍 중계, Vision AI 분석 데이터 수신, 통계/예측 분석 API, AI 브리핑·챗봇 API를 제공한다.

## 기술 스택

- **Language**: Java 25
- **Framework**: Spring Boot 4.0, Spring WebFlux, Spring Data JPA, Spring Security
- **DB**: MariaDB
- **API 문서**: springdoc-openapi (Swagger UI: `/api/swagger`)
- **외부 연동**: Vertex AI (Gemini), Open-Meteo, Toss POS API

## 시작하기

### 요구사항
- JDK 25
- MariaDB
- Google Cloud 인증 정보 (Vertex AI ADC)

### 실행

```bash
./gradlew bootRun
```

`src/main/resources/application-{dev,prod}.yaml`에 DB 접속 정보 등 프로필별 설정을 구성한다. `application.yaml`의 `toss-pos` 항목은 실제 Access Key/Secret Key/Merchant ID로 대체해야 한다.

### 테스트

```bash
./gradlew test
```

## API 명세

전체 API 명세는 [API.md](./API.md) 참고. 서버 실행 후 `/api/swagger`에서 Swagger UI로도 확인 가능하다.

## 팀

**포항항** — 김명성(팀장) · 전도원 · 이호리 · 서하민 · 송준선