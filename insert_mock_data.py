#!/usr/bin/env python3
"""
삼겹살집 1달치 목업 데이터 삽입 스크립트
대상 API: POST /api/v2/vision/data
평균 방문자: 평일 ~40명, 주말 ~65명 (전체 평균 ~50명/일)
"""

import json
import random
import urllib.request
import urllib.error
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8080"
RANDOM_SEED = 42

random.seed(RANDOM_SEED)


def post_vision_data(payload: dict) -> int:
    url = f"{BASE_URL}/api/v2/vision/data"
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url, data=data,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status
    except urllib.error.HTTPError as e:
        print(f"HTTP Error {e.code}: {e.read().decode()}")
        return e.code
    except urllib.error.URLError as e:
        print(f"Connection Error: {e.reason}")
        return -1


def pick_age() -> int:
    # 삼겹살집 연령대: 20대 10%, 30대 40%, 40대 35%, 50대 15%
    r = random.random()
    if r < 0.10:
        return 20
    if r < 0.50:
        return 30
    if r < 0.85:
        return 40
    return 50


def pick_gender() -> int:
    # 삼겹살집: 60% 남성, 40% 여성
    return 1 if random.random() < 0.60 else 2


def generate_day_payload(day: datetime, is_weekend: bool) -> dict:
    total = random.randint(55, 80) if is_weekend else random.randint(32, 52)

    open_hour = 11   # 오픈 11:00
    close_hour = 22  # 마감 22:00

    people = []
    for person_id in range(1, total + 1):
        # 점심(11~14시) 25%, 저녁(17~21시) 75%
        if random.random() < 0.25:
            in_hour = random.randint(11, 13)
        else:
            in_hour = random.randint(17, 20)

        in_minute = random.randint(0, 59)
        dwell_sec = random.randint(3000, 5400)  # 50~90분

        in_dt = day.replace(hour=in_hour, minute=in_minute, second=0, microsecond=0)
        out_dt = in_dt + timedelta(seconds=dwell_sec)

        # 마감 시간 넘기지 않도록
        max_out = day.replace(hour=close_hour - 1, minute=59, second=0, microsecond=0)
        if out_dt > max_out:
            out_dt = max_out
            dwell_sec = max(60, int((out_dt - in_dt).total_seconds()))

        people.append({
            "id": person_id,
            "age": pick_age(),
            "gender": pick_gender(),
            "in": in_dt.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
            "out": out_dt.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
            "dwellTime": dwell_sec,
        })

    avg_dwell_min = round(sum(p["dwellTime"] for p in people) / len(people) / 60)

    # 저녁 피크: 18시 60%, 19시 40%
    peak_time = 18 if random.random() < 0.60 else 19

    captured_at = day.replace(hour=open_hour, minute=0, second=0, microsecond=0)
    end_at = day.replace(hour=close_hour, minute=0, second=0, microsecond=0)

    return {
        "totalCount": total,
        "peakTime": peak_time,
        "maxResponseWaitTime": random.randint(3, 8),
        "people": people,
        "maxEmptyTableTime": random.randint(10, 35),
        "coreCustomerAge": 30,       # 핵심 고객: 30대
        "coreCustomerGender": 1,     # 핵심 고객: 남성
        "avgDwellTime": avg_dwell_min,
        "justLeftCount": random.randint(0, 3),
        "capturedAt": captured_at.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
        "endAt": end_at.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
    }


def main():
    today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    start = today - timedelta(days=30)

    print("=" * 50)
    print("  삼겹살집 목업 데이터 삽입")
    print(f"  기간: {start.date()} ~ {(today - timedelta(days=1)).date()}")
    print(f"  서버: {BASE_URL}")
    print("=" * 50)

    success = 0
    for i in range(30):
        current = start + timedelta(days=i)
        is_weekend = current.weekday() >= 5
        day_label = "주말" if is_weekend else "평일"

        payload = generate_day_payload(current, is_weekend)
        visitors = payload["totalCount"]

        print(f"[{i + 1:02d}/30] {current.date()} ({day_label}) 방문자 {visitors:3d}명 ...", end=" ", flush=True)

        status = post_vision_data(payload)
        if status == 204:
            print("완료")
            success += 1
        else:
            print(f"실패 (status={status})")

    print("=" * 50)
    print(f"  결과: {success}/30 성공")
    print("=" * 50)


if __name__ == "__main__":
    main()
