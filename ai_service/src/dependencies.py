"""依赖注入"""

from .services.vision_qa_service import vision_qa_service
from .services.detection_service import detection_service


def get_vision_qa_service():
    return vision_qa_service


def get_detection_service():
    return detection_service
