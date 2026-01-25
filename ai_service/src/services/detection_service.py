"""检测服务业务逻辑实现（YOLO）
第一次需要检测时再 initialize() 懒加载 （避免启动占显存、提高稳定性）
"""

import io
import logging
import base64
from typing import Dict, Any, Optional, List

from PIL import Image, ImageDraw, ImageFont

from ..models import YoloModel
from ..schemas import DetectionRequest, DetectionResult, ClassDistribution
from ..config import model_settings

logger = logging.getLogger(__name__)


class DetectionService:
    def __init__(self):
        self.model_registry: Dict[str, YoloModel] = {}
        self.default_model_path = model_settings.DEFAULT_MODEL_PATH
        self.default_model_name = model_settings.DEFAULT_MODEL_NAME
        self._initialized: bool = False

    def initialize(self) -> None:
        if self._initialized:
            return

        if not self.default_model_path:
            logger.warning("YOLO path not configured; detection disabled")
            self._initialized = True
            return

        try:
            logger.info(f"Loading default YOLO model lazily: {self.default_model_path}")
            detector = YoloModel(
                model_path=self.default_model_path,
                model_type=self.default_model_name,
                confidence_threshold=model_settings.DEFAULT_CONFIDENCE_THRESHOLD,
            )
            if detector.load_model():
                self.model_registry[self.default_model_name] = detector
                logger.info("Default YOLO model loaded")
            else:
                logger.warning("Default YOLO model load failed; detection disabled")
        except Exception as e:
            logger.exception(f"Failed to load YOLO model: {e}")
        finally:
            self._initialized = True

    def get_model(self, model_name: str) -> Optional[YoloModel]:
        if not self._initialized:
            self.initialize()
        return self.model_registry.get(model_name)

    def preprocess_image(self, base64_image: str) -> Image.Image:
        try:
            image_data = base64.b64decode(base64_image)
            return Image.open(io.BytesIO(image_data)).convert("RGB")
        except Exception as e:
            raise ValueError(f"Invalid base64 image: {e}")

    def postprocess_detections(self, detections: List[Dict[str, Any]]) -> List[DetectionResult]:
        results: List[DetectionResult] = []
        for det in detections:
            results.append(
                DetectionResult(
                    class_id=int(det.get("class_id", -1)),
                    class_name=str(det.get("class_name", "")),
                    confidence=float(det.get("confidence", 0.0)),
                    box=list(det.get("box", [0, 0, 0, 0])),
                    area=float(det.get("area", 0.0)),
                )
            )
        return results

    def generate_class_distribution(self, detections: List[DetectionResult]) -> List[ClassDistribution]:
        counter: Dict[str, int] = {}
        for d in detections:
            counter[d.class_name] = counter.get(d.class_name, 0) + 1
        return [ClassDistribution(class_name=k, count=v) for k, v in sorted(counter.items(), key=lambda x: -x[1])]

    def draw_detections_on_image(self, image: Image.Image, detections: List[DetectionResult]) -> Image.Image:
        draw = ImageDraw.Draw(image)

        try:
            font = ImageFont.load_default()
        except Exception:
            font = None

        for det in detections:
            x1, y1, x2, y2 = det.box
            draw.rectangle([x1, y1, x2, y2], width=2)
            label = f"{det.class_name} {det.confidence:.2f}"
            if font:
                draw.text((x1, max(0, y1 - 12)), label, font=font)
            else:
                draw.text((x1, max(0, y1 - 12)), label)

        return image

    def encode_image_to_base64(self, image: Image.Image, format: str = "JPEG") -> str:
        buf = io.BytesIO()
        image.save(buf, format=format)
        return base64.b64encode(buf.getvalue()).decode("utf-8")

    def detect_objects(self, request: DetectionRequest) -> Dict[str, Any]:
        if not self._initialized:
            self.initialize()

        model = self.get_model(request.model_name or self.default_model_name)
        if model is None:
            return {
                "success": False,
                "message": "YOLO model not available",
                "detections": [],
                "class_distribution": [],
                "annotated_image": None,
            }

        image = self.preprocess_image(request.image_base64)

        # 兼容：YoloModel.detect_objects 可能返回 List 或 Dict
        raw_out = model.detect_objects(inputs={"image": image})
        if isinstance(raw_out, dict):
            raw_dets = raw_out.get("detections", []) or []
        else:
            raw_dets = raw_out

        detections = self.postprocess_detections(raw_dets)
        class_dist = self.generate_class_distribution(detections)

        annotated_b64 = None
        if request.return_annotated_image:
            annotated = self.draw_detections_on_image(image.copy(), detections)
            annotated_b64 = self.encode_image_to_base64(annotated)

        return {
            "success": True,
            "message": "Detection completed",
            "detections": [d.model_dump() for d in detections],
            "class_distribution": [c.model_dump() for c in class_dist],
            "annotated_image": annotated_b64,
        }

    def shutdown(self):
        logger.info("Shutting down detection service...")
        for _, model in list(self.model_registry.items()):
            try:
                model.unload_model()
            except Exception:
                pass
        self.model_registry.clear()
        self._initialized = False


detection_service = DetectionService()
