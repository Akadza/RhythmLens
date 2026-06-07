import os
import shutil
import subprocess
import uuid
from pathlib import Path

import yaml
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="RhythmLens Completion Service")

REPO_DIR = Path(os.environ.get("ECG_COMPLETION_REPO", "/opt/ECG-completion"))
WORK_ROOT = Path(os.environ.get("COMPLETION_WORK_ROOT", "/data/work/completion"))
WEIGHTS_DIR = Path(os.environ.get("ECG_COMPLETION_WEIGHTS_DIR", "/opt/ECG-completion/weights"))

STANDARD_LEADS = ["I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"]


class CompleteRequest(BaseModel):
    digitized_csv_path: str
    metadata_path: str | None = None
    output_dir: str | None = None
    layout: str | None = None


@app.get("/health")
def health():
    weights = list(WEIGHTS_DIR.glob("*.pt")) if WEIGHTS_DIR.exists() else []
    return {
        "status": "ok",
        "service": "completion-service",
        "repo_exists": REPO_DIR.exists(),
        "infer_exists": (REPO_DIR / "infer.py").exists(),
        "weights_dir_exists": WEIGHTS_DIR.exists(),
        "weights_count": len(weights),
    }


@app.post("/complete")
def complete(request: CompleteRequest):
    source_csv = Path(request.digitized_csv_path)

    if not source_csv.exists():
        raise HTTPException(status_code=400, detail=f"CSV not found: {source_csv}")

    if not REPO_DIR.exists():
        raise HTTPException(status_code=500, detail=f"ECG-completion repo not mounted: {REPO_DIR}")

    infer_script = REPO_DIR / "infer.py"
    if not infer_script.exists():
        raise HTTPException(status_code=500, detail=f"infer.py not found: {infer_script}")

    if not WEIGHTS_DIR.exists() or not list(WEIGHTS_DIR.glob("*.pt")):
        raise HTTPException(status_code=500, detail=f"No .pt weights found in: {WEIGHTS_DIR}")

    job_id = uuid.uuid4().hex
    job_dir = WORK_ROOT / job_id
    input_dir = job_dir / "input"
    output_dir = Path(request.output_dir) if request.output_dir else job_dir / "output"
    config_path = job_dir / "config.yaml"

    input_dir.mkdir(parents=True, exist_ok=True)
    output_dir.mkdir(parents=True, exist_ok=True)

    input_csv = input_dir / source_csv.name
    shutil.copy2(source_csv, input_csv)

    metadata_copy_path = None
    if request.metadata_path:
        metadata_path = Path(request.metadata_path)
        if metadata_path.exists():
            metadata_dir = job_dir / "metadata"
            metadata_dir.mkdir(parents=True, exist_ok=True)
            metadata_copy_path = metadata_dir / metadata_path.name
            shutil.copy2(metadata_path, metadata_copy_path)

    config = {
        "paths": {
            "input_dir": str(input_dir),
            "output_dir": str(output_dir),
            "weights_dir": str(WEIGHTS_DIR),
            "ptbxl_path": "",
            "georgia_path": "",
            "china_path": "",
        },
        "model": {
            "n_leads": 12,
            "base_channels": 64,
        },
        "processing": {
            "target_fs": 500,
            "target_length": 5000,
            "standard_leads": STANDARD_LEADS,
        },
        "output": {
            "save_csv": True,
            "save_plots": True,
            "plot_dpi": 200,
        },
    }

    with config_path.open("w", encoding="utf-8") as file:
        yaml.safe_dump(config, file, sort_keys=False, allow_unicode=True)

    cmd = [
        "python",
        "infer.py",
        "--config",
        str(config_path),
        "--device",
        "cpu",
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
        raise HTTPException(status_code=504, detail="ECG-completion inference timed out") from exc
    except subprocess.CalledProcessError as exc:
        raise HTTPException(
            status_code=500,
            detail={
                "message": "ECG-completion inference failed",
                "stdout": exc.stdout[-4000:],
                "stderr": exc.stderr[-4000:],
            },
        ) from exc

    result_csv_files = sorted(
        output_dir.glob("*.csv"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )

    result_html_files = sorted(
        output_dir.glob("*.html"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )

    if not result_csv_files:
        raise HTTPException(
            status_code=500,
            detail={
                "message": "ECG-completion finished but no CSV output was found",
                "output_dir": str(output_dir),
                "stdout": result.stdout[-4000:],
                "stderr": result.stderr[-4000:],
            },
        )

    return {
        "status": "ok",
        "completed_csv_path": str(result_csv_files[0]),
        "plot_html_path": str(result_html_files[0]) if result_html_files else None,
        "output_dir": str(output_dir),
        "layout": request.layout,
        "metadata_path": str(metadata_copy_path) if metadata_copy_path else None,
        "stdout": result.stdout[-3000:],
    }
