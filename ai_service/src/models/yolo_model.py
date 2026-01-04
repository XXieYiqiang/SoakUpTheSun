"""YOLO模型实现"""
import os
import logging
from typing import Dict, Any, List, Optional
import numpy as np
from PIL import Image

from .base import DetectionModel
from ..config import model_settings

# 配置日志
logger = logging.getLogger(__name__)


class YoloModel(DetectionModel):
    """
    YOLO模型实现
    使用YOLO模型进行物体检测
    """
    
    def __init__(self, model_path: str, model_type: str = "yolov11x", 
                 confidence_threshold: float = 0.5, 
                 class_names_path: Optional[str] = None):
        """
        初始化YOLO模型
        
        Args:
            model_path: 模型文件路径
            model_type: 模型类型，默认为yolov11x
            confidence_threshold: 置信度阈值
            class_names_path: 类别名称文件路径
        """
        super().__init__(model_path, model_type, confidence_threshold)
        
        self.input_size = model_settings.DEFAULT_IMAGE_INPUT_SIZE
        
        if class_names_path and os.path.exists(class_names_path):
            self._load_class_names(class_names_path)
        else:
            self.class_names = self._get_default_coco_classes()
        
        self._import_yolo_dependencies()
    
    def _import_yolo_dependencies(self):
        """
        导入YOLO相关依赖
        延迟导入以避免启动时的不必要依赖检查
        """
        try:
            # 使用ultralytics库的YOLO实现
            global YOLO
            from ultralytics import YOLO
        except ImportError as e:
            logger.error(f"无法导入YOLO依赖: {e}")
            raise ImportError("请安装ultralytics库: pip install ultralytics")
    
    def _load_class_names(self, class_names_path: str):
        """
        从文件加载类别名称
        
        Args:
            class_names_path: 类别名称文件路径
        """
        try:
            with open(class_names_path, 'r', encoding='utf-8') as f:
                self.class_names = [line.strip() for line in f.readlines()]
            logger.info(f"成功加载 {len(self.class_names)} 个类别名称")
        except Exception as e:
            logger.error(f"加载类别名称失败: {e}")
            # 使用默认类别
            self.class_names = self._get_default_coco_classes()
    
    def _get_default_coco_classes(self) -> List[str]:
        """
        获取默认的COCO数据集类别
        
        Returns:
            List[str]: COCO数据集类别名称列表
        """
        return [
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
        ]
    
    def load_model(self) -> bool:
        """
        加载YOLO模型
        
        Returns:
            bool: 加载成功返回True，否则返回False
        """
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
            logger.info(f"成功加载YOLO模型")
            return True
        except Exception as e:
            logger.error(f"加载YOLO模型失败: {e}")
            self.is_loaded = False
            return False
    
    def predict(self, inputs: Dict[str, Any]) -> Dict[str, Any]:
        """
        执行YOLO模型预测
        
        Args:
            inputs: 输入数据字典，必须包含'image'键
            
        Returns:
            Dict[str, Any]: 预测结果字典
        """
        if not self.is_loaded or self.model is None:
            raise RuntimeError("模型未加载，请先调用load_model()")
        
        try:
            # 获取输入图像
            if 'image' not in inputs:
                raise ValueError("输入必须包含'image'键")
            
            # 获取图像并确保是PIL图像格式
            image = inputs['image']
            if isinstance(image, np.ndarray):
                # 如果是numpy数组，转换为PIL图像
                # 假设数组是RGB格式
                if image.shape[0] == 1:  # 处理批次维度
                    image = image[0]
                image = Image.fromarray((image * 255).astype(np.uint8))
            
            # 执行推理
            results = self.model(image, 
                                conf=self.confidence_threshold,
                                imgsz=self.input_size,
                                verbose=False)
            
            # 处理结果
            processed_results = []
            for result in results:
                # 提取检测框信息
                boxes = result.boxes
                if boxes is not None:
                    for box in boxes:
                        # 获取检测框坐标
                        x1, y1, x2, y2 = box.xyxy[0].tolist()
                        # 获取置信度
                        confidence = box.conf[0].item()
                        # 获取类别ID
                        class_id = int(box.cls[0].item())
                        # 获取类别名称
                        class_name = self.class_names[class_id] if class_id < len(self.class_names) else f'class_{class_id}'
                        
                        processed_results.append({
                            'class_id': class_id,
                            'class_name': class_name,
                            'confidence': confidence,
                            'box': [x1, y1, x2, y2],
                            'area': (x2 - x1) * (y2 - y1)
                        })
            
            return {
                'detections': processed_results,
                'total_detections': len(processed_results),
                'image_shape': image.size
            }
        except Exception as e:
            logger.error(f"YOLO预测失败: {e}")
            raise
    
    def unload_model(self) -> bool:
        """
        卸载模型，释放资源
        
        Returns:
            bool: 卸载成功返回True，否则返回False
        """
        try:
            self.model = None
            self.is_loaded = False
            logger.info("成功卸载YOLO模型")
            return True
        except Exception as e:
            logger.error(f"卸载YOLO模型失败: {e}")
            return False
    
    def postprocess_output(self, output: Dict[str, Any]) -> Dict[str, Any]:
        """
        后处理模型输出
        
        Args:
            output: 模型原始输出
            
        Returns:
            Dict[str, Any]: 处理后的输出结果
        """
        # 过滤低置信度检测结果
        filtered_detections = [
            det for det in output.get('detections', [])
            if det['confidence'] >= self.confidence_threshold
        ]
        
        # 按置信度排序
        filtered_detections.sort(key=lambda x: x['confidence'], reverse=True)
        
        # 统计每个类别的检测数量
        class_counts = {}
        for det in filtered_detections:
            class_name = det['class_name']
            class_counts[class_name] = class_counts.get(class_name, 0) + 1
        
        return {
            'detections': filtered_detections,
            'total_detections': len(filtered_detections),
            'class_distribution': class_counts,
            'image_shape': output.get('image_shape'),
            'model_info': self.get_model_info()
        }
