"""API路由定义"""

from fastapi import APIRouter
from .vision_qa import router as vision_qa_router

api_router = APIRouter(prefix="/api")
api_router.include_router(vision_qa_router)

__all__ = ["api_router"]