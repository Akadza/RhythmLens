import os
import shutil
import subprocess
import uuid
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="RhythmLens Digitizer Service")

REPO_DIR = Path(os.environ.get("OPEN_ECG_DIGITIZER_REPO", "/opt/Open-ECG-Digitizer"))
WORK_ROOT = Path(os.environ.get("DIGITIZER_WORK_ROOT", "/data/work/digitizer"))
CONFIG_TEMPLATE = Path(
    os.environ.get(
        "OPEN_ECG_DIGITIZER_CONFIG",
        "/service/config/inference_wrapper_cpu.yml",
    )
)


class DigitizeRequest(BaseModel):
    source_path: str
    output_dir: str | None = None


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "digitizer-service",
        "repo_exists": REPO_DIR.exists(),
        "digitize_exists": (REPO_DIR / "src" / "digitize.py").exists(),
        "config_exists": CONFIG_TEMPLATE.exists(),
        "unet_weight_exists": (REPO_DIR / "weights" / "unet_weights_07072025.pt").exists(),
        "lead_weight_exists": (REPO_DIR / "weights" / "lead_name_unet_weights_07072025.pt").exists(),
    }


@app.post("/digitize")
def digitize(request: DigitizeRequest):
    source_path = Path(request.source_path)

    if not source_path.exists():
        raise HTTPException(status_code=400, detail=f"Source image not found: {source_path}")

    if not REPO_DIR.exists():
        raise HTTPException(status_code=500, detail=f"Open-ECG-Digitizer repo not mounted: {REPO_DIR}")

    if not (REPO_DIR / "src" / "digitize.py").exists():
        raise HTTPException(status_code=500, detail=f"src/digitize.py not found in: {REPO_DIR}")

    if not CONFIG_TEMPLATE.exists():
        raise HTTPException(status_code=500, detail=f"Digitizer config not found: {CONFIG_TEMPLATE}")

    job_id = uuid.uuid4().hex
    job_dir = WORK_ROOT / job_id
    input_dir = job_dir / "input"
    output_dir = Path(request.output_dir) if request.output_dir else job_dir / "output"

    input_dir.mkdir(parents=True, exist_ok=True)
    output_dir.mkdir(parents=True, exist_ok=True)

    input_image = input_dir / source_path.name
    shutil.copy2(source_path, input_image)

    cmd = [
        "python",
        "-m",
        "src.digitize",
        "--config",
        str(CONFIG_TEMPLATE),
        f"DATA.images_path={input_dir}",
        f"DATA.output_path={output_dir}",
        "DATA.save_mode=all",
        "DATA.clear_output_dir_if_exists=True",
        "MODEL.KWARGS.device=cpu",
        "MODEL.KWARGS.config.LAYOUT_IDENTIFIER.KWARGS.device=cpu",
    ]

    try:
        result = subprocess.run(
            cmd,
            cwd=str(REPO_DIR),
            capture_output=True,
            text=True,
            timeout=1800,
            check=True,
        )
    except subprocess.TimeoutExpired as exc:
        raise HTTPException(status_code=504, detail="Open-ECG-Digitizer timed out") from exc
    except subprocess.CalledProcessError as exc:
        raise HTTPException(
            status_code=500,
            detail={
                "message": "Open-ECG-Digitizer failed",
                "stdout": exc.stdout[-4000:],
                "stderr": exc.stderr[-4000:],
            },
        ) from exc

    csv_files = sorted(
        output_dir.rglob("*_timeseries_canonical.csv"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )

    metadata_files = sorted(
        output_dir.rglob("digitization_metadata.csv"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )

    png_files = sorted(
        output_dir.rglob("*.png"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )

    if not csv_files:
        raise HTTPException(
            status_code=500,
            detail={
                "message": "Digitizer finished but no timeseries CSV was found",
                "output_dir": str(output_dir),
                "stdout": result.stdout[-4000:],
                "stderr": result.stderr[-4000:],
            },
        )

    return {
        "status": "ok",
        "digitized_csv_path": str(csv_files[0]),
        "metadata_path": str(metadata_files[0]) if metadata_files else None,
        "debug_png_path": str(png_files[0]) if png_files else None,
        "output_dir": str(output_dir),
        "stdout": result.stdout[-3000:],
    }
