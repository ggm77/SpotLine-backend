"""
목업 데이터 생성 스크립트
- VisionData + VisionPerson 데이터를 POST /api/v2/vision/data 로 삽입
  (multipart/form-data — "data" 파트에 JSON. weather/temperature 는 서버가
   capturedAt 기준으로 조회·저장하므로 전송하지 않음)
- 하루 8개 스냅샷 (2시간 단위, 오전 9시 ~ 오후 11시)
- 카페 기준 방문 패턴 반영

사용법:
    pip install requests
    python scripts/generate_mock_data.py [--host http://localhost:8080]
"""

import argparse
import json
import random
import sys
from datetime import datetime, timedelta

import requests

# ── 설정 ─────────────────────────────────────────────────────────────────────

SNAPSHOT_HOURS = [9, 11, 13, 15, 17, 19, 21, 22]   # 스냅샷 시작 시각(시)
SNAPSHOT_DURATION = 2                                # 스냅샷 길이(시간)

# 시간대별 방문자 수 기준 (평일) — 20~35명 범위 기준으로 설정
WEEKDAY_BASE = {9: 21, 11: 26, 13: 31, 15: 25, 17: 23, 19: 28, 21: 22, 22: 20}
# 주말 보정 (1.05배 — 완만하게)
WEEKEND_MULTIPLIER = 1.05

# 날씨 → (weather 문자열, 온도 범위, 방문자 감소율) — 감소폭 축소
WEATHERS = [
    ("SUNNY",  (22, 28), 1.00),
    ("CLOUDY", (18, 24), 0.93),
    ("RAINY",  (15, 20), 0.85),
    ("SNOW",   (0,   5), 0.80),
]
WEATHER_WEIGHTS = [0.45, 0.30, 0.20, 0.05]

# 방문자 나이대 분포 (카페 기준)
AGE_DIST  = [10, 10, 20, 20, 20, 30, 30, 40, 50]
# 성별 분포 (1=남, 2=여, 카페 기준 여성 비율 높음)
GENDER_DIST = [1, 1, 2, 2, 2]


# ── 헬퍼 ─────────────────────────────────────────────────────────────────────

def fmt(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%dT%H:%M:%S")


def make_people(snapshot_start: datetime, snapshot_end: datetime, count: int) -> list:
    people = []
    for track_id in range(1, count + 1):
        age    = random.choice(AGE_DIST)
        gender = random.choice(GENDER_DIST)
        dwell  = random.randint(300, 3600)          # 5분~60분 (초)
        in_at  = snapshot_start + timedelta(seconds=random.randint(0, SNAPSHOT_DURATION * 3600 - dwell))
        out_at = in_at + timedelta(seconds=dwell)
        if out_at > snapshot_end:
            out_at = None                            # 아직 매장에 있는 상태

        people.append({
            "id":       track_id,
            "age":      age,
            "gender":   gender,
            "in":       fmt(in_at),
            "out":      fmt(out_at) if out_at else None,
            "dwellTime": dwell,
        })
    return people


def make_snapshot(day: datetime, hour: int, visitor_count: int) -> dict:
    start = day.replace(hour=hour, minute=0, second=0, microsecond=0)
    end   = start + timedelta(hours=SNAPSHOT_DURATION)

    people       = make_people(start, end, visitor_count)
    ages         = [p["age"] for p in people if p["age"]]
    genders      = [p["gender"] for p in people if p["gender"]]
    dwell_times  = [p["dwellTime"] for p in people]

    core_age    = max(set(ages),    key=ages.count)    if ages    else None
    core_gender = max(set(genders), key=genders.count) if genders else None
    avg_dwell   = round(sum(dwell_times) / len(dwell_times) / 60) if dwell_times else None

    # VisionDataRequestDto 가 받는 필드만 전송한다.
    return {
        "totalCount":            visitor_count,
        "maxResponseWaitTime":   random.randint(1, 10),
        "maxEmptyTableTime":     random.randint(5, 30),
        "coreCustomerAge":       core_age,
        "coreCustomerGender":    core_gender,
        "avgDwellTime":          avg_dwell,
        "justLeftCount":         random.randint(0, max(1, visitor_count // 8)),
        "capturedAt":            fmt(start),
        "endAt":                 fmt(end),
        "people":                people,
    }


# ── 메인 ─────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="http://localhost:8080", help="서버 주소")
    parser.add_argument("--dry-run", action="store_true", help="실제 요청 없이 JSON만 출력")
    args = parser.parse_args()

    url       = f"{args.host}/api/v2/vision/data"
    start_day = datetime(2026, 4, 1)
    end_day   = datetime(2026, 6, 5)
    success   = 0
    failed    = 0

    total_days = (end_day - start_day).days + 1
    for day_offset in range(total_days):
        day          = start_day + timedelta(days=day_offset)
        is_weekend   = day.weekday() >= 5
        # weather_name 은 방문자 수 변동(rain_factor)·로그 표시에만 사용 (전송 X)
        weather_name, _temp_range, rain_factor = random.choices(WEATHERS, weights=WEATHER_WEIGHTS)[0]
        # 기간 전체에 걸쳐 완만한 우상향 추세 (1.0 → 1.12)
        trend = 1.0 + 0.12 * (day_offset / max(total_days - 1, 1))

        for hour in SNAPSHOT_HOURS:
            base_count   = WEEKDAY_BASE[hour]
            if is_weekend:
                base_count = int(base_count * WEEKEND_MULTIPLIER)
            # 변동폭 ±8%, 결과는 20~35명으로 클램핑
            visitor_count = max(20, min(35, int(base_count * rain_factor * trend * random.uniform(0.92, 1.08))))

            payload = make_snapshot(day, hour, visitor_count)

            if args.dry_run:
                print(json.dumps(payload, ensure_ascii=False, indent=2))
                continue

            try:
                files = {"data": (None, json.dumps(payload).encode("utf-8"), "application/json")}
                resp = requests.post(url, files=files, timeout=10)
                resp.raise_for_status()
                success += 1
                print(f"  ✓ {day.date()} {hour:02d}시  방문자 {visitor_count}명  날씨 {weather_name}")
            except Exception as e:
                failed += 1
                print(f"  ✗ {day.date()} {hour:02d}시  오류: {e}", file=sys.stderr)

    if not args.dry_run:
        total = success + failed
        print(f"\n완료: {success}/{total} 성공" + (f", {failed}건 실패" if failed else ""))


if __name__ == "__main__":
    main()
