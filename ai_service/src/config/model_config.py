"""模型配置文件（ModelSettings）
- Florence-2（GPU）
- Ollama（CPU 跑 GGUF，由 Ollama 管理）
- YOLO（可选、条件触发）
"""

import os
import logging
from typing import Tuple
from pydantic_settings import BaseSettings

logger = logging.getLogger(__name__)


def _project_root() -> str:
    return os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


class ModelSettings(BaseSettings):
    PROJECT_ROOT: str = _project_root()

    # ---------------- Florence-2 (GPU) ----------------
    FLORENCE_MODEL_DIR: str = os.path.join(PROJECT_ROOT, "models", "vlm", "Florence-2-base-ft")
    FLORENCE_DEVICE: str = "cuda"
    FLORENCE_DTYPE: str = "float16"          # float16 / bfloat16 / float32
    FLORENCE_MAX_NEW_TOKENS: int = 128

    # Caption 任务：更适合自然场景
    # 可选："<CAPTION>" / "<DETAILED_CAPTION>" / "<MORE_DETAILED_CAPTION>"
    FLORENCE_CAPTION_TASK: str = "<DETAILED_CAPTION>"

    # OCR（自然场景可能偶尔有路牌/告示，建议默认开）
    FLORENCE_ENABLE_OCR: bool = True
    FLORENCE_OCR_MAX_CHARS: int = 600

    # 为了速度与稳定：可控缩放（None 表示不缩放）
    # 推荐：384 或 448。太大显存压力会变高
    FLORENCE_RESIZE_MAX_SIDE: int = 768  # 把图片最长边限制到 768（保持比例）

    # ---------------- Ollama (LLM) ----------------
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "qwen25-1.5b-local"

    # 生成控制：口播风格，短输出
    OLLAMA_MAX_TOKENS: int = 200
    OLLAMA_TEMPERATURE: float = 0.25
    OLLAMA_TOP_P: float = 0.9

    # ---------------- YOLO (Optional) ----------------
    DEFAULT_MODEL_DIR: str = os.path.join(PROJECT_ROOT, "models", "yolo")
    DEFAULT_MODEL_NAME: str = "yolo11x"
    DEFAULT_MODEL_PATH: str = os.path.join(DEFAULT_MODEL_DIR, "yolo11x.pt")

    # 稍微提高阈值，减少误检（你可以按体验再调）
    DEFAULT_CONFIDENCE_THRESHOLD: float = 0.35
    DEFAULT_IMAGE_INPUT_SIZE: Tuple[int, int] = (640, 640)

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


model_settings = ModelSettings()


def validate_model_config() -> bool:
    try:
        if not os.path.isdir(model_settings.FLORENCE_MODEL_DIR):
            logger.error(f"Florence model dir not found: {model_settings.FLORENCE_MODEL_DIR}")
            return False

        if not (0.0 <= model_settings.DEFAULT_CONFIDENCE_THRESHOLD <= 1.0):
            logger.error("DEFAULT_CONFIDENCE_THRESHOLD must be in [0,1]")
            return False

        if not os.path.isfile(model_settings.DEFAULT_MODEL_PATH):
            logger.warning(f"YOLO model not found (will disable YOLO): {model_settings.DEFAULT_MODEL_PATH}")

        return True
    except Exception as e:
        logger.exception(f"validate_model_config failed: {e}")
        return False


def init_model_config() -> bool:
    try:
        ok = validate_model_config()
        if ok:
            logger.info("Model config initialized")
        return ok
    except Exception as e:
        logger.exception(f"init_model_config failed: {e}")
        return False
