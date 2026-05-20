"""영상 분석 메인 파이프라인."""
import cv2
import numpy as np
from datetime import datetime
from pathlib import Path

from config import (
    YOLO_MODEL, YOLO_CONF,
    PROCESS_EVERY_N_FRAMES, AGE_GENDER_EVERY_N_FRAMES,
    CONGESTION_SAMPLE_SECONDS,
)
from pipeline.zone_monitor import build_zone_polygon, is_in_zone
from pipeline.age_gender import estimate, age_to_group, majority_vote
from schemas import (
    DailyAnalysis, VideoMetadata, Summary, GenderDistribution,
    AgeDistribution, PersonRecord, EntranceEvent, CongestionPoint,
)

CONGESTION_LOW_MAX = 4
CONGESTION_MED_MAX = 9


def _seconds_to_ts(seconds: float) -> str:
    h = int(seconds // 3600)
    m = int((seconds % 3600) // 60)
    s = seconds % 60
    return f"{h:02d}:{m:02d}:{s:06.3f}"


def _peak_congestion(counts: list[int]) -> str:
    if not counts:
        return "low"
    peak = max(counts)
    if peak > CONGESTION_MED_MAX:
        return "high"
    elif peak > CONGESTION_LOW_MAX:
        return "medium"
    return "low"


def analyze(video_path: str) -> DailyAnalysis:
    from ultralytics import YOLO

    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        raise FileNotFoundError(f"영상을 열 수 없습니다: {video_path}")

    fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    duration = total_frames / fps

    zone_polygon = build_zone_polygon(width, height)
    model = YOLO(YOLO_MODEL)

    track_data: dict[int, dict] = {}
    congestion_raw: list[dict] = []
    sample_interval_frames = int(fps * CONGESTION_SAMPLE_SECONDS)
    last_sample_frame = -sample_interval_frames

    frame_idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        if frame_idx % PROCESS_EVERY_N_FRAMES != 0:
            frame_idx += 1
            continue

        ts = frame_idx / fps

        results = model.track(
            frame, persist=True, tracker="bytetrack.yaml",
            classes=[0], conf=YOLO_CONF, verbose=False,
        )

        current_ids: list[int] = []

        if results[0].boxes is not None and results[0].boxes.id is not None:
            boxes = results[0].boxes.xyxy.cpu().numpy()
            ids = results[0].boxes.id.cpu().numpy().astype(int)

            for box, tid in zip(boxes, ids):
                tid = int(tid)
                x1, y1, x2, y2 = map(int, box)
                center = ((x1 + x2) // 2, (y1 + y2) // 2)
                current_ids.append(tid)

                if tid not in track_data:
                    track_data[tid] = {
                        "first_frame": frame_idx,
                        "last_frame": frame_idx,
                        "genders": [],
                        "ages": [],
                        "zone_entry_frame": None,
                    }

                track_data[tid]["last_frame"] = frame_idx

                if track_data[tid]["zone_entry_frame"] is None and is_in_zone(center, zone_polygon):
                    track_data[tid]["zone_entry_frame"] = frame_idx

                if frame_idx % AGE_GENDER_EVERY_N_FRAMES == 0:
                    head_y2 = y1 + max(1, (y2 - y1) // 2)
                    head_crop = frame[max(0, y1):head_y2, max(0, x1):min(width, x2)]
                    gender, age = estimate(head_crop)
                    if gender:
                        track_data[tid]["genders"].append(gender)
                    if age is not None:
                        track_data[tid]["ages"].append(age)

        if frame_idx >= last_sample_frame + sample_interval_frames:
            last_sample_frame = frame_idx
            congestion_raw.append({"timestamp_seconds": ts, "person_count": len(current_ids)})

        frame_idx += 1

    cap.release()

    # ── 집계 ────────────────────────────────────────────────────────
    persons: list[PersonRecord] = []
    gender_counts = {"male": 0, "female": 0, "unknown": 0}
    age_counts = {"zeros": 0, "tens": 0, "twenties": 0, "thirties": 0, "forties": 0, "fifties_plus": 0, "unknown": 0}
    dwell_times: list[float] = []

    for tid, data in track_data.items():
        dwell = (data["last_frame"] - data["first_frame"]) / fps
        dwell_times.append(dwell)

        gender = majority_vote(data["genders"]) or "unknown"
        age_vals = data["ages"]
        avg_age = float(np.mean(age_vals)) if age_vals else None
        age_group = age_to_group(avg_age) if avg_age is not None else "unknown"

        gender_counts[gender] = gender_counts.get(gender, 0) + 1
        age_key = {
            "00s": "zeros", "10s": "tens", "20s": "twenties", "30s": "thirties",
            "40s": "forties", "50s+": "fifties_plus", "unknown": "unknown"
        }.get(age_group, "unknown")
        age_counts[age_key] += 1

        entry_frame = data["zone_entry_frame"]
        entrance_event = (
            EntranceEvent(event="enter", timestamp=_seconds_to_ts(entry_frame / fps))
            if entry_frame is not None else None
        )

        persons.append(PersonRecord(
            track_id=tid,
            gender=gender,
            age_group=age_group,
            first_seen=_seconds_to_ts(data["first_frame"] / fps),
            last_seen=_seconds_to_ts(data["last_frame"] / fps),
            dwell_time_seconds=round(dwell, 3),
            entrance_event=entrance_event,
        ))

    return DailyAnalysis(
        date="",
        video_metadata=VideoMetadata(
            filename=Path(video_path).name,
            duration_seconds=round(duration, 3),
            fps=round(fps, 3),
            resolution=f"{width}x{height}",
            processed_at=datetime.now().isoformat(timespec="seconds"),
        ),
        summary=Summary(
            total_visitors=len(track_data),
            peak_congestion=_peak_congestion([c["person_count"] for c in congestion_raw]),
            avg_dwell_time_seconds=round(float(np.mean(dwell_times)) if dwell_times else 0.0, 3),
            gender_distribution=GenderDistribution(**gender_counts),
            age_distribution=AgeDistribution(**age_counts),
        ),
        persons=persons,
        congestion_timeline=[
            CongestionPoint(
                timestamp=_seconds_to_ts(c["timestamp_seconds"]),
                person_count=c["person_count"],
            )
            for c in congestion_raw
        ],
    )
