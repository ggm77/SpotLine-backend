from pydantic import BaseModel
from typing import Optional


class VideoMetadata(BaseModel):
    filename: str
    duration_seconds: float
    fps: float
    resolution: str
    processed_at: str


class GenderDistribution(BaseModel):
    male: int
    female: int
    unknown: int


class AgeDistribution(BaseModel):
    zeros: int       # 00s (0~9세)
    tens: int        # 10s (10~19세)
    twenties: int
    thirties: int
    forties: int
    fifties_plus: int
    unknown: int


class Summary(BaseModel):
    total_visitors: int
    peak_congestion: str
    avg_dwell_time_seconds: float
    gender_distribution: GenderDistribution
    age_distribution: AgeDistribution


class EntranceEvent(BaseModel):
    event: str       # "enter"
    timestamp: str   # HH:MM:SS.mmm


class PersonRecord(BaseModel):
    track_id: int
    gender: str      # "male" | "female" | "unknown"
    age_group: str   # "teens" | "20s" | "30s" | "40s" | "50s+" | "unknown"
    first_seen: str  # HH:MM:SS.mmm
    last_seen: str   # HH:MM:SS.mmm
    dwell_time_seconds: float
    entrance_event: Optional[EntranceEvent] = None


class CongestionPoint(BaseModel):
    timestamp: str
    person_count: int


class DailyAnalysis(BaseModel):
    date: str
    video_metadata: VideoMetadata
    summary: Summary
    persons: list[PersonRecord]
    congestion_timeline: list[CongestionPoint]
