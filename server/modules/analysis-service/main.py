import json
import os
import shutil
import subprocess
import uuid
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="RhythmLens Analysis Service")

REPO_DIR = Path(os.environ.get("ECGFOUNDER_REPO", "/opt/ECGFounder"))
WORK_ROOT = Path(os.environ.get("ANALYSIS_WORK_ROOT", "/data/work/analysis"))
DEFAULT_CHECKPOINT = Path(
    os.environ.get(
        "ECGFOUNDER_CHECKPOINT",
        "/opt/ECGFounder/checkpoint/12_lead_ECGFounder.pth",
    )
)


class AnalyzeRequest(BaseModel):
    csv_path: str
    output_dir: str | None = None
    checkpoint_path: str | None = None
    threshold: float = 0.3


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "analysis-service",
        "repo_exists": REPO_DIR.exists(),
        "infer_exists": (REPO_DIR / "infer.py").exists(),
        "checkpoint_exists": DEFAULT_CHECKPOINT.exists(),
    }


@app.post("/analyze")
def analyze(request: AnalyzeRequest):
    source_csv = Path(request.csv_path)

    if not source_csv.exists():
        raise HTTPException(
            status_code=400,
            detail=f"CSV file not found: {source_csv}",
        )

    if not REPO_DIR.exists():
        raise HTTPException(
            status_code=500,
            detail=f"ECGFounder repository is not mounted: {REPO_DIR}",
        )

    infer_script = REPO_DIR / "infer.py"
    if not infer_script.exists():
        raise HTTPException(
            status_code=500,
            detail=f"ECGFounder infer.py not found: {infer_script}",
        )

    checkpoint = Path(request.checkpoint_path) if request.checkpoint_path else DEFAULT_CHECKPOINT
    if not checkpoint.exists():
        raise HTTPException(
            status_code=500,
            detail=f"ECGFounder checkpoint not found: {checkpoint}",
        )

    job_id = uuid.uuid4().hex
    job_dir = WORK_ROOT / job_id
    input_dir = job_dir / "input"
    output_dir = Path(request.output_dir) if request.output_dir else job_dir / "output"

    input_dir.mkdir(parents=True, exist_ok=True)
    output_dir.mkdir(parents=True, exist_ok=True)

    input_csv = input_dir / source_csv.name
    shutil.copy2(source_csv, input_csv)

    cmd = [
        "python",
        "infer.py",
        "--input_dir",
        str(input_dir),
        "--output_dir",
        str(output_dir),
        "--checkpoint",
        str(checkpoint),
        "--threshold",
        str(request.threshold),
    ]

    try:
        result = subprocess.run(
            cmd,
            cwd=str(REPO_DIR),
            capture_output=True,
            text=True,
            timeout=1200,
            check=True,
        )
    except subprocess.TimeoutExpired as exc:
        raise HTTPException(
            status_code=504,
            detail="ECGFounder analysis timed out",
        ) from exc
    except subprocess.CalledProcessError as exc:
        raise HTTPException(
            status_code=500,
            detail={
                "message": "ECGFounder analysis failed",
                "stdout": exc.stdout[-4000:],
                "stderr": exc.stderr[-4000:],
            },
        ) from exc

    result_files = sorted(
        output_dir.glob("*_results.json"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )

    if not result_files:
        raise HTTPException(
            status_code=500,
            detail={
                "message": "ECGFounder finished but results JSON was not found",
                "output_dir": str(output_dir),
                "stdout": result.stdout[-4000:],
                "stderr": result.stderr[-4000:],
            },
        )

    result_path = result_files[0]

    with open(result_path, "r", encoding="utf-8") as file:
        predictions = json.load(file)

    return {
        "status": "ok",
        "result_path": str(result_path),
        "output_dir": str(output_dir),
        "html_report_path": str(result_path).replace("_results.json", "_report.html"),
        "predictions": predictions,
        "stdout": result.stdout[-2000:],
    }
