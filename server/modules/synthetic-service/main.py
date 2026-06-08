import csv
import math
from pathlib import Path

from fastapi import FastAPI, HTTPException
from PIL import Image, ImageDraw, ImageFont
from pydantic import BaseModel

app = FastAPI(title="RhythmLens Synthetic ECG Service")


class SyntheticRequest(BaseModel):
    record_base_path: str | None = None
    csv_path: str | None = None
    metadata_path: str | None = None
    layout: str | None = None
    output_dir: str | None = None
    seed: int = 42


class SyntheticResponse(BaseModel):
    image_path: str
    layout: str
    rhythm_lead: str
    format: str = "png"


LEADS = ["I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"]
LAYOUT_3X4_RHYTHM_II = [
    ["I", "aVR", "V1", "V4"],
    ["II", "aVL", "V2", "V5"],
    ["III", "aVF", "V3", "V6"],
]
RHYTHM_LEAD = "II"

MM_TO_PX = 8
SMALL_MM = 1
LARGE_MM = 5
SPEED_MM_PER_SECOND = 25
GAIN_MM_PER_MV = 10
SEGMENT_SECONDS = 2.5
TOTAL_SECONDS = 10.0

PAPER_WIDTH_MM = 270
PAPER_HEIGHT_MM = 120
MARGIN_LEFT_MM = 10
MARGIN_TOP_MM = 7
ROW_HEIGHT_MM = 24
RHYTHM_ROW_TOP_MM = 82
SEGMENT_WIDTH_MM = 62.5

DISPLAY_TARGET_MV = 0.70
DISPLAY_MAX_MV = 1.05

PAPER_BACKGROUND = (255, 252, 250)
SMALL_GRID_COLOR = (255, 224, 224)
LARGE_GRID_COLOR = (255, 185, 185)
TRACE_COLOR = (25, 25, 25)
TEXT_COLOR = (35, 35, 35)


@app.get("/health")
def health():
    return {"status": "ok", "service": "synthetic-service"}


@app.post("/generate", response_model=SyntheticResponse)
def generate(request: SyntheticRequest):
    csv_path = Path(request.csv_path or "")
    if not csv_path.exists():
        raise HTTPException(status_code=404, detail="Completed ECG CSV file not found")

    layout = normalize_layout(request.layout or read_layout_from_metadata(request.metadata_path))
    if layout != "3x4+1R":
        layout = "3x4+1R"

    output_dir = Path(request.output_dir or csv_path.parent / "synthetic")
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / "synthetic_ecg.png"

    _, values_by_lead = read_completed_csv(csv_path)
    render_3x4_with_rhythm(
        values_by_lead=values_by_lead,
        output_path=output_path,
        title=f"RhythmLens · {layout}",
        footer="25 mm/s · 10 mm/mV · display-normalized completed ECG signal",
    )

    return SyntheticResponse(
        image_path=str(output_path),
        layout=layout,
        rhythm_lead=RHYTHM_LEAD,
    )


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
        reader = csv.DictReader(file)
        for row in reader:
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

    if math.isnan(parsed) or math.isinf(parsed):
        return None

    return parsed


def read_completed_csv(path: Path) -> tuple[list[float], dict[str, list[float | None]]]:
    time_values: list[float] = []
    values_by_lead: dict[str, list[float | None]] = {lead: [] for lead in LEADS}

    with path.open("r", encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        for index, row in enumerate(reader):
            time_values.append(parse_float(row.get("#")) or index / 500.0)
            for lead in LEADS:
                values_by_lead[lead].append(parse_float(row.get(lead)))

    return time_values, values_by_lead


def render_3x4_with_rhythm(
    values_by_lead: dict[str, list[float | None]],
    output_path: Path,
    title: str,
    footer: str,
) -> None:
    width_px = PAPER_WIDTH_MM * MM_TO_PX
    height_px = PAPER_HEIGHT_MM * MM_TO_PX
    image = Image.new("RGB", (width_px, height_px), PAPER_BACKGROUND)
    draw = ImageDraw.Draw(image)

    draw_grid(draw, width_px, height_px)
    font = ImageFont.load_default()
    draw.text((MARGIN_LEFT_MM * MM_TO_PX, 3 * MM_TO_PX), title, fill=TEXT_COLOR, font=font)

    for row_index, row in enumerate(LAYOUT_3X4_RHYTHM_II):
        baseline_mm = MARGIN_TOP_MM + 12 + row_index * ROW_HEIGHT_MM
        for column_index, lead in enumerate(row):
            start_time = column_index * SEGMENT_SECONDS
            end_time = start_time + SEGMENT_SECONDS
            x0_mm = MARGIN_LEFT_MM + column_index * SEGMENT_WIDTH_MM
            draw.text(
                (int(x0_mm * MM_TO_PX), int((baseline_mm - 10) * MM_TO_PX)),
                lead,
                fill=TEXT_COLOR,
                font=font,
            )
            draw_lead_segment(
                draw=draw,
                values=values_by_lead.get(lead, []),
                start_time=start_time,
                end_time=end_time,
                x0_mm=x0_mm,
                baseline_mm=baseline_mm,
            )

    rhythm_baseline_mm = RHYTHM_ROW_TOP_MM + 12
    draw.text(
        (MARGIN_LEFT_MM * MM_TO_PX, int((rhythm_baseline_mm - 10) * MM_TO_PX)),
        RHYTHM_LEAD,
        fill=TEXT_COLOR,
        font=font,
    )
    draw_lead_segment(
        draw=draw,
        values=values_by_lead.get(RHYTHM_LEAD, []),
        start_time=0.0,
        end_time=TOTAL_SECONDS,
        x0_mm=MARGIN_LEFT_MM,
        baseline_mm=rhythm_baseline_mm,
    )

    draw.text((MARGIN_LEFT_MM * MM_TO_PX, int((PAPER_HEIGHT_MM - 6) * MM_TO_PX)), footer, fill=TEXT_COLOR, font=font)
    image.save(output_path)


def draw_grid(draw: ImageDraw.ImageDraw, width_px: int, height_px: int) -> None:
    small = SMALL_MM * MM_TO_PX
    large = LARGE_MM * MM_TO_PX

    for x in range(0, width_px + 1, small):
        color = SMALL_GRID_COLOR if x % large else LARGE_GRID_COLOR
        draw.line([(x, 0), (x, height_px)], fill=color, width=1)

    for y in range(0, height_px + 1, small):
        color = SMALL_GRID_COLOR if y % large else LARGE_GRID_COLOR
        draw.line([(0, y), (width_px, y)], fill=color, width=1)


def draw_lead_segment(
    draw: ImageDraw.ImageDraw,
    values: list[float | None],
    start_time: float,
    end_time: float,
    x0_mm: float,
    baseline_mm: float,
) -> None:
    if not values:
        return

    sample_rate = len(values) / TOTAL_SECONDS
    start_index = max(0, int(start_time * sample_rate))
    end_index = min(len(values), int(end_time * sample_rate))
    if end_index <= start_index:
        return

    normalized_values = normalize_values_to_mv(values)
    normalized_segment = normalized_values[start_index:end_index]
    points = []

    for offset, value_mv in enumerate(normalized_segment):
        if value_mv is None:
            continue
        sample_index = start_index + offset
        time_seconds = sample_index / sample_rate
        local_time = time_seconds - start_time
        x_mm = x0_mm + local_time * SPEED_MM_PER_SECOND
        y_mm = baseline_mm - value_mv * GAIN_MM_PER_MV
        points.append((int(x_mm * MM_TO_PX), int(y_mm * MM_TO_PX)))

    if len(points) >= 2:
        draw.line(points, fill=TRACE_COLOR, width=2)


def normalize_values_to_mv(values: list[float | None]) -> list[float | None]:
    present = [value for value in values if value is not None]
    if not present:
        return [None for _ in values]

    sorted_values = sorted(present)
    center = sorted_values[len(sorted_values) // 2]
    deviations = sorted(abs(value - center) for value in present)
    robust_amplitude = percentile(deviations, 0.98) or 1.0
    scale = max(robust_amplitude / DISPLAY_TARGET_MV, 1e-9)

    return [
        None if value is None else clamp((value - center) / scale, -DISPLAY_MAX_MV, DISPLAY_MAX_MV)
        for value in values
    ]


def percentile(values: list[float], fraction: float) -> float:
    if not values:
        return 0.0

    index = int((len(values) - 1) * fraction)
    return values[max(0, min(index, len(values) - 1))]


def clamp(value: float, minimum: float, maximum: float) -> float:
    return max(minimum, min(maximum, value))
