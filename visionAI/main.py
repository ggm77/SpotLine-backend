"""FastAPI 서버 - 하루 단위 영상 분석."""
import asyncio
import re
from functools import partial

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

from pathlib import Path
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



@app.get("/analyze", response_model=DailyAnalysis)
async def analyze_date(path: str):

    if not Path(path).exists():
        raise HTTPException(
            status_code=404,
            detail=f"영상 파일을 찾을 수 없습니다: {path}",
        )

    loop = asyncio.get_event_loop()
    try:
        result = await loop.run_in_executor(
            None, partial(analyze, path)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"분석 중 오류 발생: {str(e)}")

    return JSONResponse(content=result.model_dump())
