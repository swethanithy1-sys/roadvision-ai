---
title: RoadVision AI Detection Service
emoji: 🛣️
colorFrom: blue
colorTo: green
sdk: docker
app_port: 7860
---

# RoadVision AI Detection Service

FastAPI + YOLOv8 microservice that performs the real computer-vision detection
`MockAIDetectionServiceImpl` stands in for in the main app. Deliberately scoped
to raw detection only — class name, confidence, normalized bounding box.
Severity, repair priority, and cost stay in the Spring Boot backend.

## Getting a pothole-detection model

This service needs a YOLOv8 `.pt` weights file trained (or fine-tuned) to
detect road damage. You have two free options:

1. **Roboflow Universe** — search for a public "pothole detection" project,
   open a trained version, and export/download the weights as
   **YOLOv8 PyTorch** format.
2. **Hugging Face Hub** — search for a community pothole/road-damage YOLOv8
   model and download its `.pt` file.

Place the file at `ai-service/weights/pothole.pt` (or point `MODEL_PATH` at
wherever you put it — see below). `weights/*.pt` is gitignored; the file isn't
committed to this repo.

## Running locally

```bash
cd ai-service
python -m venv .venv && source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

`GET /health` reports whether the model loaded. `POST /detect` accepts a
multipart `image` field and returns raw detections.

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `MODEL_PATH` | `weights/pothole.pt` | Path to the YOLOv8 weights file |
| `CONFIDENCE_THRESHOLD` | `0.25` | Minimum detection confidence (0–1) kept in the response |

## Deploying to Hugging Face Spaces (free)

1. Create a free account at [huggingface.co](https://huggingface.co) and a
   new Space with the **Docker** SDK.
2. Push this `ai-service/` directory's contents to the Space's git repo
   (the `README.md` frontmatter above is what tells Spaces to use Docker on
   port 7860 — keep it).
3. Upload your `pothole.pt` weights file to the Space (via the Files tab, or
   commit it directly — Spaces repos support Git LFS for large binaries).
4. Once the Space builds and shows "Running", note its URL —
   `https://<your-space>.hf.space` — and set it as `AI_SERVICE_URL` in the
   backend's environment (see the root README's Environment Variables
   section).

Free CPU Spaces sleep after 48h of inactivity; the next request triggers a
cold start (usually well under a minute) before serving normally.
