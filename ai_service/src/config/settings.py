"""应用配置文件（Settings）
- 负责基础配置、日志配置
"""

import logging
from pydantic_settings import BaseSettings

logger = logging.getLogger(__name__)


class Settings(BaseSettings):
    # 基础
    APP_NAME: str = "Vision Assist API"
    VERSION: str = "1.0.0"
    DEBUG: bool = False

    # 日志
    LOG_LEVEL: str = "INFO"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()


def setup_logging() -> None:
    level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)
    logging.basicConfig(
        level=level,
        format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
    )
    logger.info("Logging initialized")


def init_config() -> bool:
    try:
        setup_logging()
        logger.info("Config initialized")
        return True
    except Exception as e:
        logger.exception(f"init_config failed: {e}")
        return False
