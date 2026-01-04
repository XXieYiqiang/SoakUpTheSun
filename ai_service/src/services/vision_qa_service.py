"""视觉问答服务（面向视障用户的自然语言输出）

修复重点：
- 取消 <answer> 标签强约束（避免模型输出“...”）
- 若 LLM 输出过短/省略号：回退到 Florence caption 的模板回答（保证稳定）
- 轻清洗：只去掉明显污染行，不做激进截断
"""

import io
import base64
import logging
import re
from typing import Optional, Dict, Any, List

from PIL import Image

from ..config import model_settings
from ..models.vlm_model import FlorenceVLM
from ..models.llm_model import OllamaLLM
from ..services.detection_service import detection_service
from ..schemas.detection import DetectionRequest

logger = logging.getLogger(__name__)


def _decode_base64_image(image_base64: str) -> Image.Image:
    data = base64.b64decode(image_base64)
    return Image.open(io.BytesIO(data)).convert("RGB")


def _truncate(text: str, max_chars: int) -> str:
    text = (text or "").strip()
    if len(text) <= max_chars:
        return text
    return text[: max_chars - 3].rstrip() + "..."


def _normalize_space(s: str) -> str:
    s = (s or "").strip()
    s = re.sub(r"[ \t]+", " ", s)
    s = re.sub(r"\n{3,}", "\n\n", s)
    return s.strip()


def _looks_bad(text: str) -> bool:
    """判断 LLM 输出是否“无效/偷懒”"""
    t = (text or "").strip()
    if not t:
        return True
    # 只有省略号/标点
    if re.fullmatch(r"[\.…。，,！!？?\s]+", t):
        return True
    # 太短
    if len(t) < 18:
        return True
    return False


def _light_clean(text: str) -> str:
    """轻清洗：去掉常见污染，不做激进截断"""
    t = _normalize_space(text)
    if not t:
        return t

    # 去掉明显模板污染
    bad_phrases = [
        "以上内容为示例",
        "实际回答时需根据图片具体内容进行调整",
        "请提供更多信息",
        "最终口播答案：",
        "注意：以上内容",
        "注意：",
        "提示：",
    ]
    for bp in bad_phrases:
        t = t.replace(bp, "")

    # 去掉编号列表行（但不影响正文）
    lines = re.split(r"\n+", t)
    kept = []
    for line in lines:
        s = line.strip()
        if not s:
            continue
        if re.match(r"^\d+(\.|、|\))\s*", s):
            continue
        kept.append(s)

    out = " ".join(kept).strip()
    out = _normalize_space(out)

    # 如果最后没标点，补句号（但不要对“...”这种做）
    if out and out[-1] not in "。！？!?":
        out += "。"

    # 控制长度（别太长）
    if len(out) > 900:
        out = out[:900].rstrip() + "…"
    return out


def _fallback_from_caption(caption: str, ocr_text: str) -> str:
    """不用 LLM 时的稳定口播模板（保证永不输出垃圾）"""
    cap = _normalize_space(caption)
    ocr = _normalize_space(ocr_text)

    # 给一点结构，但仍然像口播
    parts = []
    # 尝试从 caption 猜室内/室外（非常粗略，不确定就不说）
    if any(k in cap for k in ["room", "indoors", "interior", "室内", "房间", "窗户", "桌子", "椅子"]):
        parts.append("这张图片看起来是在室内。")
    elif any(k in cap for k in ["outdoors", "street", "road", "sky", "室外", "街道", "马路"]):
        parts.append("这张图片看起来是在室外。")

    if cap:
        parts.append(f"我看到：{cap}。")

    if ocr:
        parts.append(f"图中可读到的文字大致是：{ocr}。")

    # 如果什么都没有（极少情况）
    if not parts:
        return "抱歉喵，我没能提取到足够的图像信息。你可以换一张更清晰的照片再试一次。"

    out = " ".join(parts)
    out = _normalize_space(out)
    if out and out[-1] not in "。！？!?":
        out += "。"
    return out


class VisionQAService:
    def __init__(self):
        self.vlm: Optional[FlorenceVLM] = None
        self.llm: Optional[OllamaLLM] = None
        self._initialized = False

    def initialize(self) -> None:
        if self._initialized:
            return

        self.vlm = FlorenceVLM(
            model_dir=model_settings.FLORENCE_MODEL_DIR,
            device=model_settings.FLORENCE_DEVICE,
            dtype=model_settings.FLORENCE_DTYPE,
            max_new_tokens=model_settings.FLORENCE_MAX_NEW_TOKENS,
            resize_max_side=model_settings.FLORENCE_RESIZE_MAX_SIDE,
        )
        self.vlm.load()

        self.llm = OllamaLLM(
            base_url=model_settings.OLLAMA_BASE_URL,
            model=model_settings.OLLAMA_MODEL,
            max_tokens=model_settings.OLLAMA_MAX_TOKENS,
            temperature=model_settings.OLLAMA_TEMPERATURE,
            top_p=model_settings.OLLAMA_TOP_P,
        )
        self.llm.load()

        self._initialized = True
        logger.info("VisionQAService initialized")

    def shutdown(self) -> None:
        try:
            if self.vlm:
                self.vlm.unload()
        except Exception:
            pass
        try:
            if self.llm:
                self.llm.unload()
        except Exception:
            pass
        self._initialized = False
        logger.info("VisionQAService shutdown")

    def _need_yolo(self, question: str) -> bool:
        q = (question or "").strip()
        triggers = [
            "多少", "几", "数量", "数一数",
            "左边", "右边", "上面", "下面", "中间", "角落", "旁边", "附近",
            "哪个", "哪一个", "分别", "位置", "在哪里",
            "最近", "最远", "前面", "后面",
            "危险", "障碍", "台阶", "楼梯", "路口", "车辆", "车",
        ]
        return any(k in q for k in triggers)

    def _build_prompt(
        self,
        question: str,
        caption: str,
        ocr_text: str,
        yolo_detections: Optional[List[Dict[str, Any]]],
    ) -> str:
        q = question.strip() if question else "请用简洁、清晰的中文描述这张图片的主要内容，适合视障用户理解。"

        evidence_parts = []
        if caption:
            evidence_parts.append(f"【图像描述】{caption}")
        if ocr_text:
            evidence_parts.append(f"【图中文字】{ocr_text}")
        if yolo_detections:
            topk = yolo_detections[:10]
            det_lines = []
            for d in topk:
                name = d.get("class_name", "")
                conf = float(d.get("confidence", 0.0))
                box = d.get("box", [])
                det_lines.append(f"{name}({conf:.2f}, box={box})")
            evidence_parts.append("【检测结果】" + "；".join(det_lines))

        evidence = "\n".join(evidence_parts).strip()
        logger.info(f"evidence: {evidence}")

        #我服了，蠢逼不听话
        prompt = f"""
                图像信息：{evidence}
                你是面向视障用户的中文视觉辅助助手。你必须严格依据“图像信息”回答{q}，不允许编造不存在的细节。不要重复套话。
                尽量说明这是室内还是室外、主要物体是什么.
                输出内容简洁明了，1到3句纯中文，不要重复输出相同内容。不要重复。
                请输出一段流畅自然的中文纯文本，不要输出 JSON，不要输出列表符号，不要输出英文。

                回答问题：{q}
                """
        return prompt

    def process_vision_qa(self, image_base64: str, question: Optional[str] = None) -> str:
        if not self._initialized:
            self.initialize()
        if self.vlm is None or self.llm is None:
            raise RuntimeError("Service not initialized")

        try:
            image = _decode_base64_image(image_base64)
        except Exception as e:
            logger.exception(f"Image decode failed: {e}")
            return "抱歉喵，我没能读到这张图片（可能文件损坏或格式不支持）。你可以换一张图片再试试。"

        caption = ""
        try:
            caption = self.vlm.caption(image, task=model_settings.FLORENCE_CAPTION_TASK)
            caption = _normalize_space(caption)
        except Exception as e:
            logger.exception(f"Florence caption failed: {e}")
            caption = ""

        ocr_text = ""
        if model_settings.FLORENCE_ENABLE_OCR:
            try:
                ocr_text = self.vlm.ocr(image)
                ocr_text = _truncate(_normalize_space(ocr_text), model_settings.FLORENCE_OCR_MAX_CHARS)
                if ocr_text == "-":
                    ocr_text = ""
            except Exception as e:
                logger.exception(f"Florence OCR failed: {e}")
                ocr_text = ""

        if not caption and not ocr_text:
            return "抱歉喵，我这次没能成功识别图片内容（可能强光、反光、遮挡或推理失败）。你可以换个角度或更清晰的照片再试一次。"

        yolo_detections = None
        if self._need_yolo(question or "") and model_settings.DEFAULT_MODEL_PATH:
            try:
                req = DetectionRequest(
                    image_base64=image_base64,
                    model_name=model_settings.DEFAULT_MODEL_NAME,
                    return_annotated_image=False,
                )
                det_res = detection_service.detect_objects(req)
                if det_res.get("success"):
                    yolo_detections = det_res.get("detections", [])
            except Exception as e:
                logger.exception(f"YOLO detection failed: {e}")
                yolo_detections = None

        prompt = self._build_prompt(
            question=question or "",
            caption=caption,
            ocr_text=ocr_text,
            yolo_detections=yolo_detections,
        )

        raw = ""
        try:
            raw = self.llm.generate(prompt)
        except Exception as e:
            logger.exception(f"LLM generate failed: {e}")
            raw = ""

        raw = _normalize_space(raw)
        cleaned = _light_clean(raw)

        # 关键：如果 LLM 输出无效，就回退到稳定模板（不会出现 ...。）
        if _looks_bad(cleaned):
            return _fallback_from_caption(caption, ocr_text)

        return cleaned.strip()


vision_qa_service = VisionQAService()
