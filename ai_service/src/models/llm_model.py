"""LLM 封装：通过 HTTP 调用 Ollama
要点：
- 只保留轻量 stop，避免截断有效内容
- 适度 repeat_penalty 降低重复
"""

import logging
from typing import Optional, List, Dict, Any
import requests

logger = logging.getLogger(__name__)


class OllamaLLM:
    def __init__(
        self,
        base_url: str,
        model: str,
        max_tokens: int = 150,
        temperature: float = 0.15,
        top_p: float = 0.85,
        timeout_s: int = 180,
    ):
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.max_tokens = max_tokens
        self.temperature = temperature
        self.top_p = top_p
        self.timeout_s = timeout_s

    def load(self) -> None:
        try:
            r = requests.get(f"{self.base_url}/api/tags", timeout=10)
            if r.status_code == 200:
                logger.info("Ollama reachable")
            else:
                logger.warning(f"Ollama reachable but /api/tags status={r.status_code}")
        except Exception as e:
            logger.warning(f"Ollama not reachable on load(): {e}")

    def unload(self) -> None:
        return

    def generate(self, prompt: str, stop: Optional[List[str]] = None) -> str:
        # 用 </answer> 做 stop，这傻逼模型调了老久
        stop = stop or [
            "</answer>", 
            "</s>", 
            "<|endoftext|>",
            "\n\n",
            "？",
            "根据",
            "因此",
            "综上",
            "假设",
            "【图像描述】",
            "适合视障",
            "</s>",
            "<|endoftext|>",]

        payload: Dict[str, Any] = {
            "model": self.model,
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": self.temperature,
                "top_p": self.top_p,
                "num_predict": self.max_tokens,
                "repeat_penalty": 1.12,
            },
        }

        url = f"{self.base_url}/api/generate"
        resp = requests.post(url, json=payload, timeout=self.timeout_s)
        resp.raise_for_status()
        data = resp.json()
        text = (data.get("response") or "").strip()

        # stop 截断（如果模型已经输出 </answer>，这里会更干净）
        for s in stop:
            if s and s in text:
                text = text.split(s, 1)[0].strip()

        return text
