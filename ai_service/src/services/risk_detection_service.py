import time
import math
import logging
from dataclasses import dataclass, field
from typing import Dict, Any, List, Optional, Tuple

from ..config import model_settings
from ..services.detection_service import detection_service
from ..schemas.detection import DetectionRequest

logger = logging.getLogger(__name__)

DEFAULT_RISK_CLASSES = {
    'person', 'bicycle', 'car', 'motorcycle', 'airplane', 'bus', 'train', 
            'truck', 'boat', 'traffic light', 'fire hydrant', 'stop sign',
            'parking meter', 'bench', 'bird', 'cat', 'dog', 'horse', 'sheep',
            'cow', 'elephant', 'bear', 'zebra', 'giraffe', 'backpack', 'umbrella',
            'handbag', 'tie', 'suitcase', 'frisbee', 'skis', 'snowboard',
            'sports ball', 'kite', 'baseball bat', 'baseball glove', 'skateboard',
            'surfboard', 'tennis racket', 'bottle', 'wine glass', 'cup', 'fork',
            'knife', 'spoon', 'bowl', 'banana', 'apple', 'sandwich', 'orange',
            'broccoli', 'carrot', 'hot dog', 'pizza', 'donut', 'cake', 'chair',
            'couch', 'potted plant', 'bed', 'dining table', 'toilet', 'tv',
            'laptop', 'mouse', 'remote', 'keyboard', 'cell phone', 'microwave',
            'oven', 'toaster', 'sink', 'refrigerator', 'book', 'clock', 'vase',
            'scissors', 'teddy bear', 'hair drier', 'toothbrush'
}

@dataclass
class Track:
    track_id: int
    class_name: str
    box: List[float]
    confidence: float
    last_time: float
    last_scale: float
    ema_dscale: float = 0.0
    misses: int = 0


def _iou(a: List[float], b: List[float]) -> float:
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    ix1 = max(ax1, bx1)
    iy1 = max(ay1, by1)
    ix2 = min(ax2, bx2)
    iy2 = min(ay2, by2)
    iw = max(0.0, ix2 - ix1)
    ih = max(0.0, iy2 - iy1)
    inter = iw * ih
    if inter <= 0:
        return 0.0
    area_a = max(1e-6, (ax2 - ax1) * (ay2 - ay1))
    area_b = max(1e-6, (bx2 - bx1) * (by2 - by1))
    return float(inter / (area_a + area_b - inter))


def _lane_from_x(cx: float, w: float) -> str:
    if cx < w * 0.35:
        return "left"
    if cx > w * 0.65:
        return "right"
    return "center"


def _risk_level_from_ttc(ttc: Optional[float]) -> str:
    if ttc is None:
        return "SAFE"
    if ttc < 1.0:
        return "DANGER"
    if ttc < 2.0:
        return "HIGH"
    if ttc < 4.0:
        return "CAUTION"
    return "SAFE"


class SimpleIOUTracker:
    def __init__(self, iou_thresh: float = 0.25, max_misses: int = 10):
        self.iou_thresh = iou_thresh
        self.max_misses = max_misses
        self.next_id = 1
        self.tracks: Dict[int, Track] = {}

    def update(self, detections: List[Dict[str, Any]], now: float) -> List[Track]:
        for tr in self.tracks.values():
            tr.misses += 1

        used_tracks = set()
        for det in detections:
            cls = det["class_name"]
            box = det["box"]
            conf = det["confidence"]

            best_id = None
            best_iou = 0.0
            for tid, tr in self.tracks.items():
                if tid in used_tracks:
                    continue
                if tr.class_name != cls:
                    continue
                i = _iou(tr.box, box)
                if i > best_iou:
                    best_iou = i
                    best_id = tid

            if best_id is not None and best_iou >= self.iou_thresh:
                tr = self.tracks[best_id]
                tr.box = box
                tr.confidence = conf
                tr.misses = 0
                used_tracks.add(best_id)
            else:
                area = max(1.0, (box[2] - box[0]) * (box[3] - box[1]))
                scale = math.sqrt(area)
                tid = self.next_id
                self.next_id += 1
                self.tracks[tid] = Track(
                    track_id=tid,
                    class_name=cls,
                    box=box,
                    confidence=conf,
                    last_time=now,
                    last_scale=scale,
                    ema_dscale=0.0,
                    misses=0,
                )
                used_tracks.add(tid)

        dead = [tid for tid, tr in self.tracks.items() if tr.misses > self.max_misses]
        for tid in dead:
            self.tracks.pop(tid, None)

        return list(self.tracks.values())


@dataclass
class SessionState:
    tracker: SimpleIOUTracker
    last_seen: float = field(default_factory=time.time)


class RiskDetectionService:
    def __init__(self, fps: float = 5.0):
        self.fps = fps
        self.sessions: Dict[str, SessionState] = {}

    def _gc(self, now: float, ttl_s: int = 60) -> None:
        dead = [sid for sid, st in self.sessions.items() if now - st.last_seen > ttl_s]
        for sid in dead:
            self.sessions.pop(sid, None)

    def _get_or_create_session(self, session_id: str) -> SessionState:
        now = time.time()
        self._gc(now, ttl_s=60)
        if session_id in self.sessions:
            st = self.sessions[session_id]
            st.last_seen = now
            return st

        tracker = SimpleIOUTracker(iou_thresh=0.25, max_misses=int(max(8, self.fps * 2)))
        st = SessionState(tracker=tracker, last_seen=now)
        self.sessions[session_id] = st
        return st

    def assess_frame(
        self,
        image_base64: str,
        session_id: str,
        image_size: Tuple[int, int],
        risk_classes: Optional[set] = None,
    ) -> Dict[str, Any]:
        risk_classes = risk_classes or DEFAULT_RISK_CLASSES
        st = self._get_or_create_session(session_id)

        req = DetectionRequest(
            image_base64=image_base64,
            model_name=None,
            return_annotated_image=False,
        )
        det_res = detection_service.detect_objects(req)
        if not det_res.get("success"):
            # 这里顺手把 detection 的 message 带出来，方便你定位
            return {
                "risk_level": "SAFE",
                "min_ttc_seconds": None,
                "message": f"未能完成目标检测：{det_res.get('message') or det_res.get('error') or ''}".strip(),
                "objects": [],
            }

        detections_raw = det_res.get("detections", []) or []
        w, h = image_size

        dets: List[Dict[str, Any]] = []
        for d in detections_raw:
            name = (d.get("class_name") or "").strip()
            conf = float(d.get("confidence") or 0.0)
            box = d.get("box") or d.get("bbox") or []
            if len(box) != 4:
                continue
            if name not in risk_classes:
                continue
            x1, y1, x2, y2 = map(float, box)
            area = (x2 - x1) * (y2 - y1)
            if area < 20 * 20:
                continue
            dets.append({"class_name": name, "confidence": conf, "box": [x1, y1, x2, y2]})

        if not dets:
            return {
                "risk_level": "SAFE",
                "min_ttc_seconds": None,
                "message": "检测到了目标，但没有命中风险类别集合（person/car等）。",
                "objects": [],
            }

        now = time.time()
        tracks = st.tracker.update(dets, now)

        min_ttc: Optional[float] = None
        min_reason: Optional[str] = None

        for tr in tracks:
            x1, y1, x2, y2 = tr.box
            area = max(1.0, (x2 - x1) * (y2 - y1))
            scale = math.sqrt(area)

            dt = max(1e-3, now - tr.last_time)
            dscale = (scale - tr.last_scale) / dt

            alpha = 0.6
            tr.ema_dscale = alpha * dscale + (1 - alpha) * tr.ema_dscale

            approach = tr.ema_dscale > 0.6

            ttc = None
            if approach:
                ttc = scale / max(tr.ema_dscale, 1e-3)

            tr.last_time = now
            tr.last_scale = scale

            cx = (x1 + x2) / 2.0
            lane = _lane_from_x(cx, w)

            if lane == "center" and ttc is not None:
                if (min_ttc is None) or (ttc < min_ttc):
                    min_ttc = ttc
                    min_reason = f"正前方 {tr.class_name} 正在靠近（TTC≈{ttc:.1f}s）"

        risk_level = _risk_level_from_ttc(min_ttc)

        if risk_level == "DANGER":
            message = f"危险：{min_reason or '正前方存在快速接近目标'}，建议立刻减速或停下。"
        elif risk_level == "HIGH":
            message = f"高风险：{min_reason or '正前方存在接近目标'}，请谨慎前行。"
        elif risk_level == "CAUTION":
            message = f"注意：{min_reason or '前方可能有接近目标'}。"
        else:
            message = "当前未发现明显碰撞风险。"

        return {
            "risk_level": risk_level,
            "min_ttc_seconds": float(min_ttc) if min_ttc is not None else None,
            "message": message,
        }


risk_detection_service = RiskDetectionService(fps=5.0)
