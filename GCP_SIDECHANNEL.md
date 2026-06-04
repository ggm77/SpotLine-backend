# GCP 사이드채널 통합

기존 데이터 흐름(분석기 중계 / 비전 데이터 수신 / 프론트 스트림)을 **건드리지 않고**,
그 흐름을 탭해서 여러 GCP 제품으로 복사 전송하는 부가 채널입니다.

## 동작 보장

- **전용 풀 + fire-and-forget**: 모든 GCP 호출은 `gcpSideChannelExecutor`(DiscardPolicy)에서
  비동기로 돌고 예외를 삼킵니다. 원래 요청 경로(relay `.block()`, DB 저장, sink emit, 프론트 GET)의
  응답·지연·상태에는 영향이 없습니다.
- **플래그 기본 off**: `GcpProperties`의 모든 `enabled` 기본값은 `false`. 코드를 배포해도
  **켜기 전까지는 어떤 GCP 호출도, ADC 접근도 일어나지 않습니다** (= 현재 동작과 완전 동일).
- **인증**: Vertex AI와 동일한 ADC(`cloud-platform` 스코프) Bearer 토큰을 REST로 재사용 →
  새 무거운 SDK 의존성 0 (Spring Boot 4 / Java 25 호환성 리스크 없음).

> 참고: 플래그를 **켜면** 백그라운드 부가 작업(JSON 직렬화·네트워크 호출)이 추가됩니다.
> 요청 응답 자체는 그대로지만, 서버 CPU/네트워크 사용량은 늘어납니다.

## 탭 지점

| 이벤트 | 위치 | 보내는 곳 |
|---|---|---|
| 비전 데이터 수신 | `VisionController.createVisionData` | Pub/Sub, Storage(프레임), BigQuery, Logging, Monitoring, Cloud Tasks |
| 원본 청크 중계 | `VideoRelayClient.relayChunk` | Storage(청크), Pub/Sub, Monitoring |
| 기동 시 1회 | `SecretManagerLoader` | Secret Manager(access, 실패 시 yaml 폴백) |
| Cloud Tasks 콜백 | `POST /api/internal/gcp-tasks/vision` | (수신점, 로그만) |

## 1) GCP 리소스 생성 (서버/로컬 gcloud)

```bash
PROJECT=knudc-amiproudchris
REGION=asia-northeast3
BUCKET=spotline-archive          # 전역 유일해야 함. 필요시 이름 변경
SA_EMAIL=<ADC 서비스 계정 이메일> # 예: spotline@${PROJECT}.iam.gserviceaccount.com

# API 활성화
gcloud services enable \
  pubsub.googleapis.com storage.googleapis.com bigquery.googleapis.com \
  logging.googleapis.com monitoring.googleapis.com \
  secretmanager.googleapis.com cloudtasks.googleapis.com --project="$PROJECT"

# Pub/Sub 토픽
gcloud pubsub topics create spotline-vision-events spotline-chunk-events --project="$PROJECT"

# Cloud Storage 버킷
gcloud storage buckets create "gs://$BUCKET" --location="$REGION" --project="$PROJECT"

# BigQuery 데이터셋 + 테이블 (payload는 JSON 문자열을 STRING으로 저장)
bq --location="$REGION" mk --dataset "${PROJECT}:spotline"
bq mk --table "${PROJECT}:spotline.vision_events" ingested_at:TIMESTAMP,payload:STRING

# Cloud Tasks 큐
gcloud tasks queues create spotline-tasks --location="$REGION" --project="$PROJECT"

# Secret Manager (toss-pos 시크릿 키를 옮기는 예시)
printf '%s' "<TOSS_POS_SECRET_KEY 값>" | \
  gcloud secrets create toss-pos-secret-key --data-file=- --project="$PROJECT"
```

## 2) 서비스 계정 IAM (ADC 계정에 롤 추가)

```bash
for ROLE in \
  roles/pubsub.publisher \
  roles/storage.objectAdmin \
  roles/bigquery.dataEditor \
  roles/logging.logWriter \
  roles/monitoring.metricWriter \
  roles/secretmanager.secretAccessor \
  roles/cloudtasks.enqueuer ; do
  gcloud projects add-iam-policy-binding "$PROJECT" \
    --member="serviceAccount:$SA_EMAIL" --role="$ROLE"
done

# (선택) Cloud Tasks 콜백에 OIDC 토큰을 쓸 경우에만 필요
gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --member="serviceAccount:$SA_EMAIL" --role="roles/iam.serviceAccountUser" --project="$PROJECT"
```

## 3) 켜기 (서버의 `application-prod.yaml` — gitignore라 서버에서 직접 설정)

원하는 제품만 골라 `enabled: true`. 안 적은 값은 코드 기본값을 사용합니다.

```yaml
gcp:
  project-id: knudc-amiproudchris
  location: asia-northeast3
  callback-base-url: https://spotline.seohamin.com
  task-secret: <랜덤 문자열>            # 콜백 보호용. 강력 권장
  service-account-email: ""             # OIDC 쓰려면 SA 이메일, 아니면 빈 값
  pubsub:        { enabled: true }
  storage:       { enabled: true, bucket: spotline-archive }
  bigquery:      { enabled: true }
  logging:       { enabled: true }
  monitoring:    { enabled: true }
  secret-manager:{ enabled: true }
  tasks:         { enabled: true }
```

이후 `./gradlew bootJar -x test` → 새 jar로 재기동.

## 캐비엇

- **Monitoring**: 같은 시계열에 초당 1회 이상 쓰면 429가 날 수 있습니다(무시됨). 데모용 게이지 지표입니다.
- **Storage/Pub/Sub 비용**: 프레임/청크를 통과시키면 호출·저장 비용이 지속 발생합니다. 필요 제품만 켜세요.
- **Cloud Tasks 콜백**은 `permitAll`로 공개돼 있으니 `task-secret`을 반드시 설정하세요.
