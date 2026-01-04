"""检测相关 Schema（Pydantic v2）"""

from typing import List, Optional
from pydantic import BaseModel, Field


class DetectionRequest(BaseModel):
    image_base64: str = Field(..., description="Base64 编码的图像（不含 data:image/... 前缀）")
    model_name: Optional[str] = Field(default=None, description="模型名称，默认使用配置的默认模型")
    return_annotated_image: bool = Field(default=False, description="是否返回标注后的图像 base64")


class DetectionResult(BaseModel):
    class_id: int
    class_name: str
    confidence: float
    box: List[int]  # [x1,y1,x2,y2]
    area: float


class ClassDistribution(BaseModel):
    class_name: str
    count: int
