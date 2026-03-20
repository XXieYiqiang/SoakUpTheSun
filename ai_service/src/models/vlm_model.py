"""Florence-2 视觉模型封装（GPU）

要点：
- generate 只传 input_ids + pixel_values（避免 mask shape 错误）
- pixel_values cast 到与模型一致 dtype（避免 float vs half）
- 可选 resize：更快更稳
"""

import logging
from typing import Optional, Dict, Any
from PIL import Image

import torch
from transformers import AutoProcessor, AutoModelForCausalLM
import warnings
warnings.filterwarnings("ignore", category=FutureWarning)

logger = logging.getLogger(__name__)


def _torch_dtype(dtype_name: str):
    name = (dtype_name or "").lower()
    if name in ("float16", "fp16", "half"):
        return torch.float16
    if name in ("bfloat16", "bf16"):
        return torch.bfloat16
    return torch.float32


def _resize_keep_ratio(image: Image.Image, max_side: Optional[int]) -> Image.Image:
    if not max_side or max_side <= 0:
        return image
    w, h = image.size
    m = max(w, h)
    if m <= max_side:
        return image
    scale = max_side / float(m)
    new_w = int(round(w * scale))
    new_h = int(round(h * scale))
    return image.resize((new_w, new_h), Image.BICUBIC)


class FlorenceVLM:
    def __init__(
        self,
        model_dir: str,
        device: str = "cuda",
        dtype: str = "float16",
        max_new_tokens: int = 128,
        resize_max_side: Optional[int] = None,
    ):
        self.model_dir = model_dir
        self.device = device
        self.dtype = _torch_dtype(dtype)
        self.max_new_tokens = max_new_tokens
        self.resize_max_side = resize_max_side

        self.processor: Optional[AutoProcessor] = None
        self.model: Optional[AutoModelForCausalLM] = None

    def load(self) -> None:
        logger.info(f"Loading Florence-2 from: {self.model_dir}")
        self.processor = AutoProcessor.from_pretrained(self.model_dir, trust_remote_code=True)

        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_dir,
            torch_dtype=self.dtype,
            trust_remote_code=True,
            low_cpu_mem_usage=True,
        )

        if self.device == "cuda" and torch.cuda.is_available():
            self.model = self.model.to("cuda")
        else:
            self.model = self.model.to("cpu")

        self.model.eval()
        logger.info("Florence-2 loaded")

    def unload(self) -> None:
        self.model = None
        self.processor = None
        if torch.cuda.is_available():
            torch.cuda.empty_cache()

    def _generate(self, task_prompt: str, image: Image.Image) -> str:
        if self.model is None or self.processor is None:
            raise RuntimeError("FlorenceVLM not loaded")

        image = _resize_keep_ratio(image, self.resize_max_side)

        model_inputs = self.processor(
            text=task_prompt,
            images=image,
            return_tensors="pt",
        )

        input_ids = model_inputs.get("input_ids", None)
        pixel_values = model_inputs.get("pixel_values", None)
        if input_ids is None or pixel_values is None:
            raise RuntimeError(f"Processor outputs missing keys. got={list(model_inputs.keys())}")

        dev = next(self.model.parameters()).device
        input_ids = input_ids.to(device=dev)
        pixel_values = pixel_values.to(device=dev, dtype=self.dtype)

        with torch.inference_mode():
            generated_ids = self.model.generate(
                input_ids=input_ids,
                pixel_values=pixel_values,
                max_new_tokens=self.max_new_tokens,
                do_sample=False,
            )

        generated_text = self.processor.batch_decode(
            generated_ids,
            skip_special_tokens=False,
        )[0]
        return generated_text, image  # 返回 image 给 postprocess 用（尺寸可能变化）

    def _postprocess(self, task_prompt: str, generated_text: str, image: Image.Image) -> Dict[str, Any]:
        try:
            return self.processor.post_process_generation(
                generated_text,
                task=task_prompt,
                image_size=(image.width, image.height),
            )
        except Exception:
            return {"raw": generated_text}

    def caption(self, image: Image.Image, task: str) -> str:
        text, used_image = self._generate(task, image)
        parsed = self._postprocess(task, text, used_image)

        if isinstance(parsed, dict):
            for k in (task, task.strip("<>"), "caption", "text"):
                if k in parsed and isinstance(parsed[k], str):
                    return parsed[k].strip()
        return str(parsed).strip()

    def ocr(self, image: Image.Image) -> str:
        task = "<OCR>"
        text, used_image = self._generate(task, image)
        parsed = self._postprocess(task, text, used_image)

        if isinstance(parsed, dict):
            for k in ("<OCR>", "OCR", "text"):
                if k in parsed and isinstance(parsed[k], str):
                    return parsed[k].strip()

            if "ocr" in parsed and isinstance(parsed["ocr"], list):
                parts = []
                for item in parsed["ocr"]:
                    if isinstance(item, dict) and "text" in item:
                        parts.append(str(item["text"]))
                    else:
                        parts.append(str(item))
                return "\n".join([p for p in parts if p.strip()]).strip()

        return str(parsed).strip()
