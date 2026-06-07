from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="RhythmLens Synthetic ECG Service")


class SyntheticRequest(BaseModel):
    record_base_path: str | None = None
    csv_path: str | None = None
    layout: int | None = None
    output_dir: str | None = None
    seed: int = 42


@app.get("/health")
def health():
    return {"status": "ok", "service": "synthetic-service"}


@app.post("/generate")
def generate(request: SyntheticRequest):
    raise HTTPException(
        status_code=501,
        detail="ecg-image-kit adapter is not connected yet",
    )
