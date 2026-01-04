"""视觉问答相关的数据模型"""
from typing import List, Dict, Any
from pydantic import BaseModel, Field

class QAResult(BaseModel):
    """
    视觉问答结果模型
    """
    question: str = Field(..., description="用户提问")
    answer: str = Field(..., description="回答内容")


class ServiceStatus(BaseModel):
    """
    服务状态模型
    """
    is_initialized: bool = Field(..., description="服务是否已初始化")
    services: Dict[str, Any] = Field(..., description="各服务组件状态")
    
    @property
    def is_healthy(self) -> bool:
        """判断服务是否健康"""
        return self.is_initialized


__all__ = ['QAResult', 'ServiceStatus']