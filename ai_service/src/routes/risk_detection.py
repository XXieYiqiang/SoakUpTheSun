import base64
from typing import Optional
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from PIL import Image
import io

from ..services.risk_detection_service import risk_detection_service
from ..schemas import RiskAssessResponse

router = APIRouter(prefix="/risk", tags=["RiskDetection"])


@router.post("/assess-file", response_model=RiskAssessResponse, summary="逐帧碰撞风险评估（上传单帧）")
async def assess_risk_file(
    image: UploadFile = File(..., description="上传单帧 JPG/PNG/WebP"),
    session_id: str = Form(..., description="同一段视频流请保持 session_id 不变"),
):
    if image.content_type not in ("image/jpeg", "image/png", "image/webp"):
        raise HTTPException(status_code=400, detail=f"不支持的图片类型: {image.content_type}")

    data = await image.read()
    if not data:
        raise HTTPException(status_code=400, detail="上传文件为空")

    try:
        pil = Image.open(io.BytesIO(data)).convert("RGB")
        w, h = pil.size
    except Exception:
        raise HTTPException(status_code=400, detail="图片解码失败")

    image_base64 = base64.b64encode(data).decode("utf-8")

    res = risk_detection_service.assess_frame(
        image_base64=image_base64,
        session_id=session_id,
        image_size=(w, h),
    )

    return RiskAssessResponse(
        session_id=session_id,
        fps=risk_detection_service.fps,
        risk_level=res.get("risk_level", "SAFE"),
        min_ttc_seconds=res.get("min_ttc_seconds"),
        message=res.get("message", ""),
    )
