import csv
import math
import os
import shutil
import sys
import traceback
from pathlib import Path

import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="RhythmLens Synthetic ECG Service")


class SyntheticRequest(BaseModel):
    csv_path: str | None = None
    digitized_csv_path: str | None = None
    metadata_path: str | None = None
    layout: str | None = None
    output_dir: str | None = None
    seed: int = 42


class SyntheticResponse(BaseModel):
    image_path: str
    layout: str
    rhythm_lead: str
    format: str = "png"


class StaticSampler:
    def __init__(self, value: bool):
        self.value = value

    def rvs(self):
        return self.value


LEADS = ["I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"]
RHYTHM_LEAD = "II"
TOTAL_SECONDS = 10.0
KIT_GENERATOR_DIR = Path("/opt/ecg-image-kit/codes/ecg-image-generator")
TARGET_DISPLAY_MV = 0.85
MAX_DISPLAY_MV = 2.4


@app.get("/health")
def health():
    return {"status": "ok", "service": "synthetic-service", "renderer": "ecg-image-kit"}


@app.post("/generate", response_model=SyntheticResponse)
def generate(request: SyntheticRequest):
    try:
        return generate_impl(request)
    except HTTPException:
        raise
    except Exception as exc:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"ECG-Image-Kit generation failed: {type(exc).__name__}: {exc}") from exc


def generate_impl(request: SyntheticRequest) -> SyntheticResponse:
    completed_csv_path = Path(request.csv_path or "")
    if not completed_csv_path.exists():
        raise HTTPException(status_code=404, detail="Completed ECG CSV file not found")

    layout = normalize_layout(request.layout or read_layout_from_metadata(request.metadata_path))
    if layout != "3x4+1R":
        layout = "3x4+1R"

    output_dir = Path(request.output_dir or completed_csv_path.parent / "synthetic")
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / "synthetic_ecg.png"

    digitized_csv_path = Path(request.digitized_csv_path) if request.digitized_csv_path else None
    if digitized_csv_path is not None and not digitized_csv_path.exists():
        digitized_csv_path = None

    signal_mv, sampling_rate = build_signal_for_rendering(completed_csv_path, digitized_csv_path)
    render_with_ecg_image_kit(signal_mv, sampling_rate, output_dir, output_path, request.seed, layout)

    return SyntheticResponse(image_path=str(output_path), layout=layout, rhythm_lead=RHYTHM_LEAD)


def normalize_layout(layout: str | None) -> str:
    raw = (layout or "").strip()
    return raw if raw else "3x4+1R"


def read_layout_from_metadata(metadata_path: str | None) -> str | None:
    if not metadata_path:
        return None
    path = Path(metadata_path)
    if not path.exists():
        return None
    with path.open("r", encoding="utf-8-sig", newline="") as file:
        for row in csv.DictReader(file):
            layout = row.get("lead_layout")
            if layout:
                return layout
    return None


def parse_float(value: str | None) -> float | None:
    if value is None:
        return None
    raw = value.strip()
    if raw == "":
        return None
    try:
        parsed = float(raw)
    except ValueError:
        return None
    return None if math.isnan(parsed) or math.isinf(parsed) else parsed


def read_csv_signal(path: Path):
    time_values = []
    values_by_lead = {lead: [] for lead in LEADS}
    with path.open("r", encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        for index, row in enumerate(reader):
            time_value = parse_float(row.get("#"))
            time_values.append(time_value if time_value is not None else index / 500.0)
            for lead in LEADS:
                values_by_lead[lead].append(parse_float(row.get(lead)))
    return time_values, values_by_lead


def infer_sampling_rate(time_values: list[float], sample_count: int) -> int:
    if len(time_values) >= 2:
        deltas = [time_values[i + 1] - time_values[i] for i in range(min(len(time_values) - 1, 100)) if time_values[i + 1] > time_values[i]]
        if deltas:
            median_delta = sorted(deltas)[len(deltas) // 2]
            if median_delta > 0:
                return max(1, int(round(1.0 / median_delta)))
    return max(1, int(round(sample_count / TOTAL_SECONDS)))


def build_signal_for_rendering(completed_csv_path: Path, digitized_csv_path: Path | None):
    completed_time, completed_values = read_csv_signal(completed_csv_path)
    sample_count = len(completed_values[LEADS[0]])
    if sample_count == 0:
        raise HTTPException(status_code=422, detail="Completed ECG CSV is empty")
    sampling_rate = infer_sampling_rate(completed_time, sample_count)
    merged_values = {lead: list(completed_values[lead]) for lead in LEADS}

    if digitized_csv_path is not None:
        _, digitized_values = read_csv_signal(digitized_csv_path)
        digitized_count = len(digitized_values[LEADS[0]])
        if digitized_count > 0:
            digitized_rate = digitized_count / TOTAL_SECONDS
            for lead in LEADS:
                for index in range(sample_count):
                    source_index = min(digitized_count - 1, max(0, int(round(index * digitized_rate / sampling_rate))))
                    digitized_value = digitized_values[lead][source_index]
                    if digitized_value is not None:
                        merged_values[lead][index] = digitized_value
    return normalize_signal_to_mv(merged_values), sampling_rate


def normalize_signal_to_mv(values_by_lead):
    normalized = {}
    for lead, values in values_by_lead.items():
        present = [value for value in values if value is not None]
        if not present:
            normalized[lead] = [0.0 for _ in values]
            continue
        sorted_values = sorted(present)
        center = sorted_values[len(sorted_values) // 2]
        deviations = sorted(abs(value - center) for value in present)
        robust_amplitude = percentile(deviations, 0.98) or 1.0
        scale = max(robust_amplitude / TARGET_DISPLAY_MV, 1e-9)
        normalized[lead] = [0.0 if value is None else clamp((value - center) / scale, -MAX_DISPLAY_MV, MAX_DISPLAY_MV) for value in values]
    return normalized


def render_with_ecg_image_kit(signal_mv, sampling_rate: int, output_dir: Path, output_path: Path, seed: int, layout: str):
    if not KIT_GENERATOR_DIR.exists():
        raise HTTPException(status_code=500, detail="ECG-Image-Kit is not mounted at /opt/ecg-image-kit")

    sys.path.insert(0, str(KIT_GENERATOR_DIR))
    previous_cwd = Path.cwd()
    try:
        os.chdir(KIT_GENERATOR_DIR)
        import wfdb
        from extract_leads import get_paper_ecg
        from helper_functions import read_config_file

        input_dir = output_dir / "_kit_input"
        input_dir.mkdir(parents=True, exist_ok=True)
        record_name = "rhythmlens_synthetic"
        input_base = input_dir / record_name
        signal_matrix = np.column_stack([signal_mv[lead] for lead in LEADS])

        wfdb.wrsamp(
            record_name=record_name,
            fs=sampling_rate,
            units=["mV" for _ in LEADS],
            sig_name=LEADS,
            p_signal=signal_matrix,
            fmt=["16" for _ in LEADS],
            adc_gain=[1000.0 for _ in LEADS],
            baseline=[0 for _ in LEADS],
            write_dir=str(input_dir),
            comments=["RhythmLens synthetic ECG source"],
        )

        generated_files = get_paper_ecg(
            input_file=str(input_base.with_suffix(".dat")),
            header_file=str(input_base.with_suffix(".hea")),
            output_directory=str(output_dir),
            seed=seed,
            add_dc_pulse=StaticSampler(True),
            add_bw=StaticSampler(False),
            show_grid=StaticSampler(True),
            add_print=StaticSampler(False),
            configs=read_config_file(str(KIT_GENERATOR_DIR / "config.yaml")),
            mask_unplotted_samples=False,
            start_index=0,
            store_configs=0,
            store_text_bbox=False,
            resolution=200,
            units="inches",
            papersize="",
            add_lead_names=True,
            pad_inches=0,
            font_type=str(resolve_font_path()),
            standard_colours=5,
            full_mode=RHYTHM_LEAD,
            bbox=False,
            columns=4,
        )
        if not generated_files:
            raise HTTPException(status_code=500, detail="ECG-Image-Kit did not generate an image")
        generated_path = Path(generated_files[0])
        if generated_path.resolve() != output_path.resolve():
            shutil.copyfile(generated_path, output_path)
    finally:
        os.chdir(previous_cwd)
        if str(KIT_GENERATOR_DIR) in sys.path:
            sys.path.remove(str(KIT_GENERATOR_DIR))


def resolve_font_path() -> Path:
    fonts_dir = KIT_GENERATOR_DIR / "Fonts"
    fonts = sorted(fonts_dir.glob("*.ttf")) if fonts_dir.exists() else []
    return fonts[0] if fonts else Path("Fonts/Times_New_Roman.ttf")


def percentile(values: list[float], fraction: float) -> float:
    if not values:
        return 0.0
    index = int((len(values) - 1) * fraction)
    return values[max(0, min(index, len(values) - 1))]


def clamp(value: float, minimum: float, maximum: float) -> float:
    return max(minimum, min(maximum, value))
