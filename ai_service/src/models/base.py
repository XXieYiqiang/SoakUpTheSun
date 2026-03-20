"""基础模型类定义"""
import logging
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
import numpy as np
from PIL import Image

# 配置日志
logger = logging.getLogger(__name__)


class BaseModel(ABC):
    """
    所有AI模型的基类
    定义了模型加载、推理、评估等通用接口
    """
    
    def __init__(self, model_path: str, model_type: str):
        """
        初始化模型
        
        Args:
            model_path: 模型文件路径
            model_type: 模型类型名称
        """
        self.model_path = model_path
        self.model_type = model_type
        self.model = None
        self.is_loaded = False
        
    @abstractmethod
    def load_model(self) -> bool:
        """
        加载模型
        
        Returns:
            bool: 加载成功返回True，否则返回False
        """
        pass
    
    @abstractmethod
    def predict(self, inputs: Dict[str, Any]) -> Dict[str, Any]:
        """
        执行模型预测
        
        Args:
            inputs: 输入数据字典
            
        Returns:
            Dict[str, Any]: 预测结果字典
        """
        pass
    
    @abstractmethod
    def unload_model(self) -> bool:
        """
        卸载模型，释放资源
        
        Returns:
            bool: 卸载成功返回True，否则返回False
        """
        pass
    
    def get_model_info(self) -> Dict[str, Any]:
        """
        获取模型信息
        
        Returns:
            Dict[str, Any]: 模型信息字典
        """
        return {
            "model_path": self.model_path,
            "model_type": self.model_type,
            "is_loaded": self.is_loaded
        }


class VisionModel(BaseModel):
    """
    视觉模型基类
    专门用于处理图像输入的模型
    """
    
    def __init__(self, model_path: str, model_type: str):
        """
        初始化视觉模型
        
        Args:
            model_path: 模型文件路径
            model_type: 模型类型名称
        """
        super().__init__(model_path, model_type)
        self.input_size = None
    
    def preprocess_image(self, image: Image.Image, target_size: Optional[tuple] = None) -> np.ndarray:
        """
        预处理图像
        
        Args:
            image: 输入PIL图像
            target_size: 目标尺寸 (width, height)
            
        Returns:
            np.ndarray: 预处理后的图像数组
        """
        # 调整图像大小
        if target_size:
            resized_image = image.resize(target_size, Image.BILINEAR)
        else:
            resized_image = image
        
        # 转换为numpy数组
        img_array = np.array(resized_image)
        
        # 归一化
        if img_array.max() > 1.0:
            img_array = img_array / 255.0
        
        # 添加批次维度
        if len(img_array.shape) == 3:
            img_array = np.expand_dims(img_array, axis=0)
        
        return img_array
    
    def postprocess_output(self, output: np.ndarray) -> Dict[str, Any]:
        """
        后处理模型输出
        子类应该重写此方法以提供特定的后处理逻辑
        
        Args:
            output: 模型原始输出
            
        Returns:
            Dict[str, Any]: 处理后的输出结果
        """
        return {"raw_output": output.tolist()}


class DetectionModel(VisionModel):
    """
    目标检测模型基类
    专门用于物体检测任务的模型
    """
    
    def __init__(self, model_path: str, model_type: str, confidence_threshold: float = 0.5):
        """
        初始化检测模型
        
        Args:
            model_path: 模型文件路径
            model_type: 模型类型名称
            confidence_threshold: 置信度阈值
        """
        super().__init__(model_path, model_type)
        self.confidence_threshold = confidence_threshold
        self.class_names = []
    
    def set_confidence_threshold(self, threshold: float):
        """
        设置置信度阈值
        
        Args:
            threshold: 新的置信度阈值 (0.0-1.0)
        """
        if 0.0 <= threshold <= 1.0:
            self.confidence_threshold = threshold
        else:
            raise ValueError("置信度阈值必须在0.0到1.0之间")
    
    def set_class_names(self, class_names: List[str]):
        """
        设置类别名称列表
        
        Args:
            class_names: 类别名称列表
        """
        self.class_names = class_names
    
    def detect(self, image: Image.Image) -> Dict[str, Any]:
        """
        执行目标检测
        
        Args:
            image: 输入图像
            
        Returns:
            Dict[str, Any]: 检测结果
        """
        # 预处理图像
        processed_image = self.preprocess_image(image)
        
        # 执行预测
        results = self.predict({"image": processed_image})
        
        # 后处理结果
        return self.postprocess_output(results)