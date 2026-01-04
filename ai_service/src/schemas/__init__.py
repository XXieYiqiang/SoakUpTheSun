"""数据模型模式定义"""

from .detection import (
    DetectionRequest,
    DetectionResult,
    ClassDistribution,
)

from .vision_qa import (
    QAResult,
    ServiceStatus
)

__all__ = [
    "DetectionRequest",
    "DetectionResult",
    "ClassDistribution",
    "QAResult",
    "ServiceStatus"
]