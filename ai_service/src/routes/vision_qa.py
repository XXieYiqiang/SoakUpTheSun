"""视觉问答 API 路由
- /vision-qa/qa      : JSON(base64) 输入
- /vision-qa/qa-file : 上传 JPG/PNG/WebP（Swagger 可直接选文件）
"""

import base64
import logging
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from pydantic import BaseModel, Field
from typing import Optional

from ..services import vision_qa_service

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/vision-qa", tags=["VisionQA"])


class VisionQARequest(BaseModel):
    image_base64: str = Field(..., description="Base64 编码图片（不含 data:image/... 前缀）")
    question: Optional[str] = Field(default=None, description="用户问题，如不传则默认描述图片内容")


class VisionQAResponse(BaseModel):
    question: str
    answer: str

@router.post("/qa", response_model=VisionQAResponse, summary="视觉问答（文件上传：JPG/PNG/WebP）")
async def vision_qa_file(
    file: UploadFile = File(..., description="上传 JPG/PNG/WebP 图片文件"),
    description: Optional[str] = Form(default=None, description="用户问题（可选）"),
) -> VisionQAResponse:
    if file.content_type not in ("image/jpeg", "image/png", "image/webp"):
        raise HTTPException(status_code=400, detail=f"不支持的图片类型: {file.content_type}")

    data = await file.read()
    if not data:
        raise HTTPException(status_code=400, detail="上传文件为空")

    image_base64 = base64.b64encode(data).decode("utf-8")
    q = description or "请用简洁、清晰的中文描述这张图片的主要内容"
    answer = vision_qa_service.process_vision_qa(image_base64, q)
    return VisionQAResponse(question=q, answer=answer)
