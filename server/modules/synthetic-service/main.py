from fastapi import FastAPI

app = FastAPI(title="RhythmLens Synthetic ECG Service")


@app.get("/health")
def health():
    return {"status": "ok", "service": "synthetic-service", "renderer": "ecg-image-kit"}
