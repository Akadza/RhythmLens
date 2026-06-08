from pathlib import Path
from typing import Annotated

from fastapi import Depends, HTTPException, Request, status
from fastapi.responses import FileResponse
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.main import (
    EcgRecordEntity,
    EcgStatus,
    SYNTHETIC_URL,
    app,
    ensure_ecg_access,
    get_current_user,
    get_db,
    post_json,
    utc_now,
)
from app.main import UserEntity


class SyntheticImageResponse(BaseModel):
    ecg_id: str
    image_url: str
    image_path: str
    layout: str
    rhythm_lead: str
    format: str


def build_file_url(request: Request, ecg_id: str) -> str:
    return str(request.url_for("get_synthetic_image_file", ecg_id=ecg_id))


def validate_synthetic_ready(record: EcgRecordEntity) -> None:
    if record.status != EcgStatus.PROCESSED.value:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"ECG record is not processed yet: {record.status}",
        )

    if not record.completed_csv_path:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Completed ECG CSV is not available",
        )


def generate_or_get_synthetic(record: EcgRecordEntity, db: Session) -> dict:
    if record.synthetic_image_path and Path(record.synthetic_image_path).exists():
        return {
            "image_path": record.synthetic_image_path,
            "layout": "3x4+1R",
            "rhythm_lead": "II",
            "format": "png",
        }

    if not SYNTHETIC_URL:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Synthetic ECG service is not configured",
        )

    output_dir = str(Path(record.storage_dir) / "synthetic")
    result = post_json(
        SYNTHETIC_URL + "/generate",
        {
            "csv_path": record.completed_csv_path,
            "metadata_path": record.digitization_metadata_path,
            "output_dir": output_dir,
            "layout": "from_metadata",
        },
        timeout=300,
    )

    image_path = result.get("image_path")
    if not image_path or not Path(image_path).exists():
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Synthetic ECG image was not generated",
        )

    record.synthetic_image_path = image_path
    record.updated_at = utc_now()
    db.commit()
    db.refresh(record)

    return result


@app.post("/ecg/{ecg_id}/synthetic-image", response_model=SyntheticImageResponse)
def generate_synthetic_image(
    ecg_id: str,
    request: Request,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> SyntheticImageResponse:
    record = db.get(EcgRecordEntity, ecg_id)
    if record is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="ECG record not found")

    ensure_ecg_access(record, current_user, db)
    validate_synthetic_ready(record)
    result = generate_or_get_synthetic(record, db)

    return SyntheticImageResponse(
        ecg_id=record.id,
        image_url=build_file_url(request, record.id),
        image_path=result.get("image_path", record.synthetic_image_path or ""),
        layout=result.get("layout", "3x4+1R"),
        rhythm_lead=result.get("rhythm_lead", "II"),
        format=result.get("format", "png"),
    )


@app.get("/ecg/{ecg_id}/synthetic-image/file")
def get_synthetic_image_file(
    ecg_id: str,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> FileResponse:
    record = db.get(EcgRecordEntity, ecg_id)
    if record is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="ECG record not found")

    ensure_ecg_access(record, current_user, db)

    if not record.synthetic_image_path or not Path(record.synthetic_image_path).exists():
        validate_synthetic_ready(record)
        generate_or_get_synthetic(record, db)

    if not record.synthetic_image_path or not Path(record.synthetic_image_path).exists():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Synthetic ECG image not found")

    return FileResponse(
        record.synthetic_image_path,
        media_type="image/png",
        filename=f"rhythmlens_{ecg_id}.png",
    )
