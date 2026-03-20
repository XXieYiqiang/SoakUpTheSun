from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

class RiskAssessResponse(BaseModel):
    session_id: str
    fps: float = Field(default=5.0)
    risk_level: str = Field(description="SAFE/CAUTION/HIGH/DANGER")
    min_ttc_seconds: Optional[float] = None
    message: str
