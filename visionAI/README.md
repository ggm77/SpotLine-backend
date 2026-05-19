# VisionAI Analytics

매장 방문자 영상을 분석하여 방문자 수, 성별·연령대 분포, 혼잡도 타임라인을 JSON으로 반환하는 FastAPI 서버.

---

## 프로젝트 구조

```
visionAI/
├── main.py              # FastAPI 엔드포인트
├── config.py            # 환경 설정 (서버 이전 시 수정 대상)
├── schemas.py           # Pydantic 응답 모델
├── requirements.txt     # 의존성 패키지
├── yolov8n.pt           # YOLO 모델 가중치 파일
├── videos/              # 분석 대상 영상 디렉토리
│   └── YYYY-MM-DD.mp4
└── pipeline/
    ├── analyzer.py      # 영상 분석 메인 파이프라인
    ├── age_gender.py    # InsightFace 연령/성별 추정
    └── zone_monitor.py  # 입구 구역 진입 감지
```

---

## 서버 이전 시 변경 파일

### 1. `config.py` — 반드시 확인

| 설정값 | 기본값 | 변경 필요 조건 |
|---|---|---|
| `VIDEOS_DIR` | `./videos` | 영상 저장 경로가 다를 경우 |
| `YOLO_MODEL` | `yolov8n.pt` | 다른 YOLO 모델을 사용할 경우 |
| `YOLO_CONF` | `0.4` | 탐지 정확도를 조정할 경우 |
| `PROCESS_EVERY_N_FRAMES` | `3` | 서버 성능에 따라 처리 주기 조정 |
| `AGE_GENDER_EVERY_N_FRAMES` | `15` | 서버 성능에 따라 처리 주기 조정 |
| `CONGESTION_SAMPLE_SECONDS` | `60` | 혼잡도 샘플링 간격 조정 |
| `ENTRANCE_ZONE_RELATIVE` | 우하단 40% | 카메라 설치 위치가 다를 경우 입구 구역 좌표 조정 |

**입구 구역 좌표 변경 예시** (`config.py`):
```python
# 상대 좌표 (0.0 ~ 1.0) — 카메라 앵글에 맞게 조정
ENTRANCE_ZONE_RELATIVE = [
    (0.60, 0.60),  # 좌상단
    (1.00, 0.60),  # 우상단
    (1.00, 1.00),  # 우하단
    (0.60, 1.00),  # 좌하단
]
```

### 2. `pipeline/age_gender.py` — GPU 서버로 이전 시

기본값은 CPU 추론(`CPUExecutionProvider`). GPU 서버라면 변경:

```python
# 변경 전 (CPU)
_app.prepare(ctx_id=0, det_size=(320, 320))
# providers=["CPUExecutionProvider"]  ← get_analyzer() 내부

# 변경 후 (GPU, CUDA)
_app = FaceAnalysis(name="buffalo_s", providers=["CUDAExecutionProvider"])
_app.prepare(ctx_id=0, det_size=(640, 640))  # 해상도도 높일 수 있음
```

### 3. 영상 파일 배치

영상 파일은 `videos/` 디렉토리 아래 **날짜 형식 파일명**으로 저장:
```
videos/
├── 2026-05-17.mp4
├── 2026-05-18.mp4
└── 2026-05-19.mp4
```

### 4. `yolov8n.pt` 모델 파일

서버에 파일을 함께 복사하거나, 없으면 첫 실행 시 자동 다운로드됨.  
더 정확한 모델이 필요하면 `config.py`의 `YOLO_MODEL` 값을 변경:
```python
YOLO_MODEL = "yolov8s.pt"   # small (더 정확, 더 느림)
YOLO_MODEL = "yolov8m.pt"   # medium
```

---

## 설치 및 실행

### 1. 의존성 설치

```bash
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. 서버 실행

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

개발 중 자동 리로드:
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

---

## API 사용법

### 서버 상태 확인

```
GET /health
```

응답:
```json
{ "status": "ok", "version": "1.0.0" }
```

---

### 분석 가능한 영상 목록 조회

```
GET /videos
```

응답:
```json
{ "dates": ["2026-05-17", "2026-05-18"] }
```

---

### 날짜별 영상 분석

```
GET /analyze/{date}
```

- `date`: `YYYY-MM-DD` 형식 (예: `2026-05-17`)
- 해당 날짜의 `videos/{date}.mp4` 파일을 분석
- 영상 길이에 따라 수 분 소요될 수 있음

**요청 예시:**
```bash
curl http://localhost:8000/analyze/2026-05-17
```

**응답 구조:**
```json
{
  "date": "2026-05-17",
  "video_metadata": {
    "filename": "2026-05-17.mp4",
    "duration_seconds": 3600.0,
    "fps": 25.0,
    "resolution": "1920x1080",
    "processed_at": "2026-05-19T14:00:00"
  },
  "summary": {
    "total_visitors": 42,
    "peak_congestion": "high",
    "avg_dwell_time_seconds": 180.5,
    "gender_distribution": {
      "male": 20,
      "female": 18,
      "unknown": 4
    },
    "age_distribution": {
      "zeros": 0,
      "tens": 3,
      "twenties": 15,
      "thirties": 12,
      "forties": 8,
      "fifties_plus": 4,
      "unknown": 0
    }
  },
  "persons": [
    {
      "track_id": 1,
      "gender": "male",
      "age_group": "20s",
      "first_seen": "00:01:05.000",
      "last_seen": "00:04:30.000",
      "dwell_time_seconds": 205.0,
      "entrance_event": {
        "event": "enter",
        "timestamp": "00:01:06.200"
      }
    }
  ],
  "congestion_timeline": [
    { "timestamp": "00:01:00.000", "person_count": 3 },
    { "timestamp": "00:02:00.000", "person_count": 8 }
  ]
}
```

**peak_congestion 기준:**

| 값 | 기준 |
|---|---|
| `"low"` | 최대 동시 인원 4명 이하 |
| `"medium"` | 최대 동시 인원 5~9명 |
| `"high"` | 최대 동시 인원 10명 이상 |

**에러 응답:**

| 코드 | 원인 |
|---|---|
| `422` | 날짜 형식 오류 또는 날짜 미입력 |
| `404` | 해당 날짜의 영상 파일 없음 |
| `500` | 분석 중 내부 오류 |

---

## InsightFace 모델 자동 다운로드

`age_gender.py`에서 `buffalo_s` 모델을 첫 실행 시 자동으로 다운로드합니다.  
인터넷이 차단된 서버라면 아래 경로에 미리 복사:

```
~/.insightface/models/buffalo_s/
```
