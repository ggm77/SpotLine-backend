"""입구 ROI 진입 감지 모듈."""
import cv2
import numpy as np
from config import ENTRANCE_ZONE_RELATIVE


def build_zone_polygon(width: int, height: int) -> np.ndarray:
    """상대 좌표를 픽셀 좌표로 변환하여 polygon 반환."""
    pts = [
        (int(rx * width), int(ry * height))
        for rx, ry in ENTRANCE_ZONE_RELATIVE
    ]
    return np.array(pts, dtype=np.int32)


def is_in_zone(point: tuple[int, int], polygon: np.ndarray) -> bool:
    """point(x, y)가 polygon 내부에 있는지 확인."""
    result = cv2.pointPolygonTest(polygon, (float(point[0]), float(point[1])), False)
    return result >= 0


def draw_zone(frame: np.ndarray, polygon: np.ndarray) -> np.ndarray:
    """디버그용: 프레임에 입구 구역 표시."""
    overlay = frame.copy()
    cv2.fillPoly(overlay, [polygon], (0, 255, 0))
    cv2.addWeighted(overlay, 0.3, frame, 0.7, 0, frame)
    cv2.polylines(frame, [polygon], True, (0, 255, 0), 2)
    cv2.putText(frame, "ENTRANCE", (polygon[0][0], polygon[0][1] - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
    return frame
