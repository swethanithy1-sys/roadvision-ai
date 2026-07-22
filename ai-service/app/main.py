"""
RoadVision AI - detection microservice.

Thin FastAPI wrapper around a YOLOv8 model. Deliberately dumb: it returns raw
detections (class name, confidence, normalized box) and nothing else. Severity,
repair priority, and cost estimation stay in the Spring Boot backend
(RealAIDetectionServiceImpl + RepairEstimator) — this service's only job is
computer vision, mirroring how MockAIDetectionServiceImpl is scoped.
"""

import io
import os

from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image
from pydantic import BaseModel
from ultralytics import YOLO

MODEL_PATH = os.getenv("MODEL_PATH", "weights/pothole.pt")
CONFIDENCE_THRESHOLD = float(os.getenv("CONFIDENCE_THRESHOLD", "0.25"))

app = FastAPI(title="RoadVision AI Detection Service")

_model: YOLO | None = None
_model_load_error: str | None = None

try:
    if os.path.exists(MODEL_PATH):
        _model = YOLO(MODEL_PATH)
    else:
        _model_load_error = (
            f"No weights file found at '{MODEL_PATH}'. "
            "Place a pothole-detection YOLOv8 .pt file there (see README) "
            "and restart the service."
        )
except Exception as exc:  # pragma: no cover - startup diagnostics only
    _model_load_error = f"Failed to load model from '{MODEL_PATH}': {exc}"


class Detection(BaseModel):
    class_name: str
    confidence: float
    x: float
    y: float
    width: float
    height: float


class DetectionResponse(BaseModel):
    detections: list[Detection]


@app.get("/health")
def health():
    if _model is None:
        return {"status": "degraded", "reason": _model_load_error}
    return {"status": "ok"}


@app.post("/detect", response_model=DetectionResponse)
async def detect(image: UploadFile = File(...)):
    if _model is None:
        raise HTTPException(status_code=503, detail=_model_load_error)

    raw_bytes = await image.read()
    try:
        pil_image = Image.open(io.BytesIO(raw_bytes)).convert("RGB")
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Invalid image file: {exc}") from exc

    width, height = pil_image.size
    results = _model.predict(pil_image, conf=CONFIDENCE_THRESHOLD, verbose=False)

    detections: list[Detection] = []
    for result in results:
        for box in result.boxes:
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            class_id = int(box.cls[0].item())
            class_name = result.names.get(class_id, str(class_id))
            confidence = float(box.conf[0].item()) * 100

            detections.append(
                Detection(
                    class_name=class_name,
                    confidence=round(confidence, 2),
                    x=round(x1 / width, 4),
                    y=round(y1 / height, 4),
                    width=round((x2 - x1) / width, 4),
                    height=round((y2 - y1) / height, 4),
                )
            )

    return DetectionResponse(detections=detections)
