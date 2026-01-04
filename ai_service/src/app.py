"""FastAPI应用主入口
运行命令:
uvicorn src.app:app --reload
"""

"""FastAPI 应用入口"""

import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import settings, init_config
from .config.model_config import init_model_config
from .services.vision_qa_service import vision_qa_service
from .services.detection_service import detection_service
from .routes.vision_qa import router as vision_qa_router

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_config()
    init_model_config()

    # Florence + Ollama 探测（YOLO 懒加载）
    vision_qa_service.initialize()

    yield

    try:
        detection_service.shutdown()
    except Exception:
        pass
    try:
        vision_qa_service.shutdown()
    except Exception:
        pass


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.VERSION,
    debug=settings.DEBUG,
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(vision_qa_router)


@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok"}
