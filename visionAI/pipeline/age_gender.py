"""InsightFace 기반 연령/성별 추정 모듈."""
import numpy as np

_app = None


def get_analyzer():
    global _app
    if _app is None:
        from insightface.app import FaceAnalysis
        _app = FaceAnalysis(name="buffalo_s", providers=["CPUExecutionProvider"])
        _app.prepare(ctx_id=0, det_size=(320, 320))
    return _app


def estimate(frame_crop: np.ndarray) -> tuple[str | None, float | None]:
    """
    사람 bounding box crop에서 얼굴을 찾아 성별/나이 추정.
    Returns: (gender_str, age_float) or (None, None) if no face detected.
    """
    if frame_crop is None or frame_crop.size == 0:
        return None, None

    try:
        analyzer = get_analyzer()
        faces = analyzer.get(frame_crop)
        if not faces:
            return None, None

        face = max(faces, key=lambda f: (f.bbox[2] - f.bbox[0]) * (f.bbox[3] - f.bbox[1]))
        gender = "male" if face.gender == 1 else "female"
        age = float(face.age)
        return gender, age
    except Exception:
        return None, None


def age_to_group(age: float) -> str:
    if age < 10:
        return "00s"
    elif age < 20:
        return "10s"
    elif age < 30:
        return "20s"
    elif age < 40:
        return "30s"
    elif age < 50:
        return "40s"
    return "50s+"


def majority_vote(values: list) -> str | None:
    if not values:
        return None
    return max(set(values), key=values.count)
