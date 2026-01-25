"""YOLO模型实现（Ultralytics）
兼容两种调用：
- model.predict(image: PIL.Image) -> List[Dict]
- model.detect_objects(image=..., inputs=...) -> List[Dict] 或 Dict(包含detections)
"""

import os
import logging
from typing import Dict, Any, List, Optional, Union

import numpy as np
from PIL import Image

from .base import DetectionModel
from ..config import model_settings

logger = logging.getLogger(__name__)


class YoloModel(DetectionModel):
    """
    YOLO模型实现
    使用ultralytics YOLO进行物体检测
    """

    def __init__(
        self,
        model_path: str,
        model_type: str = "yolov11x",
        confidence_threshold: float = 0.5,
        class_names_path: Optional[str] = None,
    ):
        super().__init__(model_path, model_type, confidence_threshold)

        self.input_size = model_settings.DEFAULT_IMAGE_INPUT_SIZE

        if class_names_path and os.path.exists(class_names_path):
            self._load_class_names(class_names_path)
        else:
            self.class_names = self._get_default_coco_classes()

        self._import_yolo_dependencies()

    def _import_yolo_dependencies(self):
        try:
            global YOLO
            from ultralytics import YOLO  # type: ignore
        except ImportError as e:
            logger.error(f"无法导入YOLO依赖: {e}")
            raise ImportError("请安装ultralytics库: pip install ultralytics")

    def _load_class_names(self, class_names_path: str):
        try:
            with open(class_names_path, "r", encoding="utf-8") as f:
                self.class_names = [line.strip() for line in f.readlines()]
            logger.info(f"成功加载 {len(self.class_names)} 个类别名称")
        except Exception as e:
            logger.error(f"加载类别名称失败: {e}")
            self.class_names = self._get_default_coco_classes()

    def _get_default_coco_classes(self) -> List[str]:
        return [
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train",
            "truck", "boat", "traffic light", "fire hydrant", "stop sign",
            "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep",
            "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard",
            "sports ball", "kite", "baseball bat", "baseball glove", "skateboard",
            "surfboard", "tennis racket", "bottle", "wine glass", "cup", "fork",
            "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
            "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
            "couch", "potted plant", "bed", "dining table", "toilet", "tv",
            "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
            "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
            "scissors", "teddy bear", "hair drier", "toothbrush",
        ]

    def load_model(self) -> bool:
        try:
            if not os.path.exists(self.model_path):
                auto_model_path = os.path.join(model_settings.MODELS_DIR, "yolo", "yolo11x.pt")
                if os.path.exists(auto_model_path):
                    self.model_path = auto_model_path
                else:
                    logger.error(f"模型文件不存在: {self.model_path}")
                    logger.error(f"默认位置的模型文件也不存在: {auto_model_path}")
                    return False

            self.model = YOLO(self.model_path)
            self.is_loaded = True
            logger.info(f"成功加载YOLO模型: {self.model_path}")
            return True
        except Exception as e:
            logger.error(f"加载YOLO模型失败: {e}")
            self.is_loaded = False
            return False

    # --------- 关键：实现抽象方法 predict() ---------
    def predict(self, image: Image.Image) -> List[Dict[str, Any]]:
        """
        执行YOLO推理，返回“标准化后的检测框列表”
        List[{'class_id','class_name','confidence','box','area'}]
        """
        if not self.is_loaded or self.model is None:
            raise RuntimeError("模型未加载，请先调用load_model()")

        # 确保输入是PIL RGB
        if isinstance(image, np.ndarray):
            if image.ndim == 4 and image.shape[0] == 1:
                image = image[0]
            # 假设是0~1浮点或0~255 uint8
            if image.dtype != np.uint8:
                image = (np.clip(image, 0, 1) * 255).astype(np.uint8)
            image = Image.fromarray(image)
        if not isinstance(image, Image.Image):
            raise ValueError("predict() 需要 PIL.Image 或 numpy.ndarray")

        image = image.convert("RGB")

        results = self.model(
            image,
            conf=float(self.confidence_threshold),
            imgsz=self.input_size,  
            verbose=False,
        )

        processed: List[Dict[str, Any]] = []
        for r in results:
            boxes = getattr(r, "boxes", None)
            if boxes is None:
                continue

            for b in boxes:
                x1, y1, x2, y2 = b.xyxy[0].tolist()
                conf = float(b.conf[0].item())
                cls_id = int(b.cls[0].item())
                cls_name = self.class_names[cls_id] if cls_id < len(self.class_names) else f"class_{cls_id}"
                area = float(max(0.0, (x2 - x1) * (y2 - y1)))

                processed.append(
                    {
                        "class_id": cls_id,
                        "class_name": cls_name,
                        "confidence": conf,
                        "box": [round(x1), round(y1), round(x2), round(y2)],  # 转换为整数以匹配Schema要求
                        "area": area,
                    }
                )

        # 按置信度降序，方便上层直接用
        processed.sort(key=lambda d: d["confidence"], reverse=True)
        return processed

    # --------- 兼容 DetectionService 的调用方式 ---------
    def detect_objects(
        self,
        inputs: Optional[Dict[str, Any]] = None,
        *,
        image: Optional[Union[Image.Image, np.ndarray]] = None,
        return_dict: bool = False,
    ) -> Union[List[Dict[str, Any]], Dict[str, Any]]:
        """
        兼容两种调用：
        - detect_objects(image=pil_img)  （DetectionService目前就是这么调的）
        - detect_objects({'image': pil_img})

        return_dict=True 时返回 {'detections':..., 'total_detections':..., 'image_shape':...}
        默认返回 List[det]
        """
        if image is None:
            if not inputs or "image" not in inputs:
                raise ValueError("detect_objects 需要 image=... 或 inputs={'image':...}")
            image = inputs["image"]

        dets = self.predict(image if isinstance(image, Image.Image) else image)

        if return_dict:
            # 这里 image 可能是 ndarray，尽量取 shape
            shape = None
            if isinstance(image, Image.Image):
                shape = image.size
            elif isinstance(image, np.ndarray):
                shape = (int(image.shape[1]), int(image.shape[0]))
            return {"detections": dets, "total_detections": len(dets), "image_shape": shape}

        return dets

    def unload_model(self) -> bool:
        try:
            self.model = None
            self.is_loaded = False
            logger.info("成功卸载YOLO模型")
            return True
        except Exception as e:
            logger.error(f"卸载YOLO模型失败: {e}")
            return False
