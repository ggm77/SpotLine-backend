from pathlib import Path

BASE_DIR = Path(__file__).parent
VIDEOS_DIR = BASE_DIR / "videos"

YOLO_MODEL = "yolov10m.pt"
YOLO_CONF = 0.4

# 분석 주기: N 프레임마다 YOLO 추적 실행
PROCESS_EVERY_N_FRAMES = 3
# 연령/성별 추정 주기: N 프레임마다 InsightFace 실행 (PROCESS_EVERY_N_FRAMES 배수)
AGE_GENDER_EVERY_N_FRAMES = 15

# 혼잡도 타임라인 샘플링 간격 (초)
CONGESTION_SAMPLE_SECONDS = 60

# 입구 구역 (오른쪽 하단) - 상대 좌표 (0.0 ~ 1.0)
# 프레임 우하단 40% 영역
ENTRANCE_ZONE_RELATIVE = [
    (0.60, 0.60),
    (1.00, 0.60),
    (1.00, 1.00),
    (0.60, 1.00),
]

