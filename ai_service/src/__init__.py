"""AI服务包初始化"""

# 版本信息
__version__ = "1.0.0"

# 导出主要组件
from .app import app
from .config import settings
from .routes import api_router

__all__ = [
    "app",
    "settings",
    "api_router"
]