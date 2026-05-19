"""FastAPI 서버 - 하루 단위 영상 분석."""
import asyncio
import re
from functools import partial

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

from config import VIDEOS_DIR
from pipeline.analyzer import analyze
from schemas import DailyAnalysis

app = FastAPI(title="VisionAI Analytics", version="1.0.0", redirect_slashes=False)

DATE_PATTERN = re.compile(r"^\d{4}-\d{2}-\d{2}$")


@app.get("/health")
def health():
    return {"status": "ok", "version": "1.0.0"}


@app.get("/videos")
def list_videos():
    """분석 가능한 영상 날짜 목록 반환."""
    if not VIDEOS_DIR.exists():
        return {"dates": []}
    dates = sorted(
        p.stem for p in VIDEOS_DIR.glob("*.mp4")
        if DATE_PATTERN.match(p.stem)
    )
    return {"dates": dates}


@app.get("/analyze")
@app.get("/analyze/")
def analyze_no_date():
    raise HTTPException(
        status_code=422,
        detail="날짜를 입력하세요. 예: /analyze/2026-05-17",
    )


@app.get("/analyze/{date}", response_model=DailyAnalysis)
async def analyze_date(date: str):
    """
    날짜(YYYY-MM-DD) 기준 영상을 분석하여 일일 분석 결과를 JSON으로 반환합니다.
    영상 파일은 videos/{date}.mp4 경로에 있어야 합니다.
    """
    if not DATE_PATTERN.match(date):
        raise HTTPException(
            status_code=422,
            detail="날짜 형식이 올바르지 않습니다. 형식: YYYY-MM-DD",
        )

    video_path = VIDEOS_DIR / f"{date}.mp4"
    if not video_path.exists():
        raise HTTPException(
            status_code=404,
            detail=f"영상 파일을 찾을 수 없습니다: videos/{date}.mp4",
        )

    loop = asyncio.get_event_loop()
    try:
        result = await loop.run_in_executor(
            None, partial(analyze, str(video_path), date)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"분석 중 오류 발생: {str(e)}")

    return JSONResponse(content=result.model_dump())
