from datetime import datetime, timezone
import json
import math
import os
import secrets
import shutil
import string
import urllib.error
import urllib.request
import uuid
from enum import Enum
from pathlib import Path
from typing import Annotated

import firebase_admin
from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile, status
from firebase_admin import auth, credentials
from pydantic import BaseModel, Field
from sqlalchemy import DateTime, String, Text, create_engine, or_, select
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column, sessionmaker


DATABASE_URL = os.environ["DATABASE_URL"]
FIREBASE_CREDENTIALS_PATH = os.environ["FIREBASE_CREDENTIALS_PATH"]

DIGITIZER_URL = os.environ["DIGITIZER_URL"].rstrip("/")
COMPLETION_URL = os.environ["COMPLETION_URL"].rstrip("/")
ANALYSIS_URL = os.environ["ANALYSIS_URL"].rstrip("/")
SYNTHETIC_URL = os.environ.get("SYNTHETIC_URL", "").rstrip("/")

WORK_ROOT = Path(os.environ.get("WORK_ROOT", "/data/work"))
STORAGE_ROOT = Path(os.environ.get("STORAGE_ROOT", "/data/storage"))


class UserRole(str, Enum):
    PATIENT = "PATIENT"
    DOCTOR = "DOCTOR"


class EcgStatus(str, Enum):
    UPLOADED = "UPLOADED"
    DIGITIZING = "DIGITIZING"
    COMPLETING = "COMPLETING"
    ANALYZING = "ANALYZING"
    PROCESSED = "PROCESSED"
    ERROR = "ERROR"


class Base(DeclarativeBase):
    pass


class UserEntity(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    firebase_uid: Mapped[str] = mapped_column(String, unique=True, index=True)
    email: Mapped[str] = mapped_column(String, index=True)
    full_name: Mapped[str] = mapped_column(String)
    role: Mapped[str] = mapped_column(String)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class PatientInviteEntity(Base):
    __tablename__ = "patient_invites"

    patient_user_id: Mapped[str] = mapped_column(String, primary_key=True)
    invite_code: Mapped[str] = mapped_column(String, unique=True, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class DoctorPatientLinkEntity(Base):
    __tablename__ = "doctor_patient_links"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    doctor_user_id: Mapped[str] = mapped_column(String, index=True)
    patient_user_id: Mapped[str] = mapped_column(String, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class EcgRecordEntity(Base):
    __tablename__ = "ecg_records"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    owner_user_id: Mapped[str] = mapped_column(String, index=True)
    uploaded_by_user_id: Mapped[str] = mapped_column(String, index=True)
    status: Mapped[str] = mapped_column(String, index=True)

    storage_dir: Mapped[str] = mapped_column(String)

    digitized_csv_path: Mapped[str | None] = mapped_column(String, nullable=True)
    digitization_metadata_path: Mapped[str | None] = mapped_column(String, nullable=True)

    completed_csv_path: Mapped[str | None] = mapped_column(String, nullable=True)
    completion_plot_path: Mapped[str | None] = mapped_column(String, nullable=True)

    analysis_json_path: Mapped[str | None] = mapped_column(String, nullable=True)
    analysis_report_path: Mapped[str | None] = mapped_column(String, nullable=True)
    top_predictions_json: Mapped[str | None] = mapped_column(Text, nullable=True)

    synthetic_image_path: Mapped[str | None] = mapped_column(String, nullable=True)

    error_message: Mapped[str | None] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class EcgPredictionEntity(Base):
    __tablename__ = "ecg_predictions"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    ecg_record_id: Mapped[str] = mapped_column(String, index=True)
    label: Mapped[str] = mapped_column(String)
    probability: Mapped[str] = mapped_column(String)
    detected: Mapped[str] = mapped_column(String)
    rank: Mapped[str] = mapped_column(String)
    
class DoctorConclusionEntity(Base):
    __tablename__ = "doctor_conclusions"

    ecg_id: Mapped[str] = mapped_column(String, primary_key=True)
    doctor_user_id: Mapped[str] = mapped_column(String, index=True)
    text: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class AuthSyncRequest(BaseModel):
    id_token: str = Field(alias="id_token")
    full_name: str | None = Field(default=None, alias="full_name")
    role: UserRole | None = None


class AuthUserResponse(BaseModel):
    id: str
    firebase_uid: str
    email: str
    full_name: str
    role: UserRole
    created_at: str


class PatientInviteResponse(BaseModel):
    invite_code: str


class PatientResponse(BaseModel):
    id: str
    email: str
    full_name: str
    invite_code: str
    created_at: str


class AttachPatientRequest(BaseModel):
    invite_code: str


class EcgPredictionResponse(BaseModel):
    label: str
    probability: float
    detected: bool


class EcgRecordResponse(BaseModel):
    id: str
    owner_user_id: str
    uploaded_by_user_id: str
    status: EcgStatus
    storage_dir: str

    digitized_csv_path: str | None = None
    digitization_metadata_path: str | None = None
    completed_csv_path: str | None = None
    completion_plot_path: str | None = None
    analysis_json_path: str | None = None
    analysis_report_path: str | None = None
    synthetic_image_path: str | None = None

    error_message: str | None = None
    top_predictions: list[EcgPredictionResponse] = []

    created_at: str
    updated_at: str


class PipelineRequest(BaseModel):
    source_path: str
    output_base_dir: str | None = None


class EcgSignalSegmentResponse(BaseModel):
    origin: str
    start_sample_index: int
    voltage: list[float]


class EcgSignalLeadResponse(BaseModel):
    lead: str
    origin: str
    segments: list[EcgSignalSegmentResponse]


class EcgSignalResponse(BaseModel):
    ecg_id: str
    sampling_rate: int
    duration_seconds: float
    leads: list[EcgSignalLeadResponse]

class DoctorConclusionRequest(BaseModel):
    text: str = Field(default="")


class DoctorConclusionResponse(BaseModel):
    ecg_id: str
    doctor_id: str
    text: str
    created_at: str
    updated_at: str

engine = create_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)

app = FastAPI(
    title="RhythmLens API",
    version="0.1.0",
)


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def init_firebase() -> None:
    if firebase_admin._apps:
        return

    cred = credentials.Certificate(FIREBASE_CREDENTIALS_PATH)
    firebase_admin.initialize_app(cred)


def init_database() -> None:
    Base.metadata.create_all(bind=engine)


@app.on_event("startup")
def on_startup() -> None:
    init_firebase()
    init_database()
    WORK_ROOT.mkdir(parents=True, exist_ok=True)
    STORAGE_ROOT.mkdir(parents=True, exist_ok=True)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def verify_id_token(id_token: str) -> dict:
    try:
        return auth.verify_id_token(id_token)
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Firebase ID token",
        ) from exc


def get_current_user(
    authorization: str | None = Header(default=None),
    db: Session = Depends(get_db),
) -> UserEntity:
    if authorization is None or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Bearer token is required",
        )

    id_token = authorization.removeprefix("Bearer ").strip()
    decoded_token = verify_id_token(id_token)
    firebase_uid = decoded_token["uid"]

    user = db.scalar(
        select(UserEntity).where(UserEntity.firebase_uid == firebase_uid)
    )

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User profile is not registered",
        )

    return user


def user_to_response(user: UserEntity) -> AuthUserResponse:
    return AuthUserResponse(
        id=user.id,
        firebase_uid=user.firebase_uid,
        email=user.email,
        full_name=user.full_name,
        role=UserRole(user.role),
        created_at=user.created_at.isoformat(),
    )


def require_role(user: UserEntity, role: UserRole) -> None:
    if user.role != role.value:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Only {role.value} users can perform this action",
        )


def normalize_invite_code(invite_code: str) -> str:
    return "".join(invite_code.upper().split())


def generate_invite_code(db: Session) -> str:
    alphabet = string.ascii_uppercase + string.digits

    for _ in range(20):
        left = "".join(secrets.choice(alphabet) for _ in range(4))
        right = "".join(secrets.choice(alphabet) for _ in range(4))
        code = f"RL-{left}-{right}"
        existing = db.scalar(
            select(PatientInviteEntity).where(PatientInviteEntity.invite_code == code)
        )
        if existing is None:
            return code

    return f"RL-{uuid.uuid4().hex[:8].upper()}"


def get_or_create_patient_invite(patient: UserEntity, db: Session) -> PatientInviteEntity:
    require_role(patient, UserRole.PATIENT)

    invite = db.get(PatientInviteEntity, patient.id)
    if invite is not None:
        return invite

    invite = PatientInviteEntity(
        patient_user_id=patient.id,
        invite_code=generate_invite_code(db),
        created_at=utc_now(),
    )
    db.add(invite)
    db.commit()
    db.refresh(invite)
    return invite


def patient_to_response(patient: UserEntity, db: Session) -> PatientResponse:
    invite = get_or_create_patient_invite(patient, db)
    return PatientResponse(
        id=patient.id,
        email=patient.email,
        full_name=patient.full_name,
        invite_code=invite.invite_code,
        created_at=patient.created_at.isoformat(),
    )


def doctor_patient_link_id(doctor_user_id: str, patient_user_id: str) -> str:
    return f"{doctor_user_id}:{patient_user_id}"


def is_doctor_linked_to_patient(db: Session, doctor_user_id: str, patient_user_id: str) -> bool:
    link = db.get(
        DoctorPatientLinkEntity,
        doctor_patient_link_id(doctor_user_id, patient_user_id),
    )
    return link is not None


def get_linked_patient_ids(db: Session, doctor_user_id: str) -> list[str]:
    links = db.scalars(
        select(DoctorPatientLinkEntity).where(
            DoctorPatientLinkEntity.doctor_user_id == doctor_user_id
        )
    ).all()
    return [link.patient_user_id for link in links]


def ensure_ecg_access(record: EcgRecordEntity, user: UserEntity, db: Session) -> None:
    if record.owner_user_id == user.id:
        return

    if record.uploaded_by_user_id == user.id:
        return

    if user.role == UserRole.DOCTOR.value and is_doctor_linked_to_patient(
        db=db,
        doctor_user_id=user.id,
        patient_user_id=record.owner_user_id,
    ):
        return

    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="You do not have access to this ECG record",
    )


def resolve_ecg_owner_for_upload(
    current_user: UserEntity,
    owner_user_id: str | None,
    db: Session,
) -> str:
    requested_owner_id = owner_user_id.strip() if owner_user_id else current_user.id

    if requested_owner_id == current_user.id:
        return current_user.id

    if current_user.role != UserRole.DOCTOR.value:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only doctors can upload ECG records for another patient",
        )

    patient = db.get(UserEntity, requested_owner_id)
    if patient is None or patient.role != UserRole.PATIENT.value:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found",
        )

    if not is_doctor_linked_to_patient(
        db=db,
        doctor_user_id=current_user.id,
        patient_user_id=patient.id,
    ):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Doctor is not linked to this patient",
        )

    return patient.id


def parse_predictions(raw: str | None) -> list[dict]:
    if not raw:
        return []
    try:
        value = json.loads(raw)
        return value if isinstance(value, list) else []
    except Exception:
        return []


def extract_top_predictions(analyzed: dict) -> list[dict]:
    predictions_payload = analyzed.get("predictions")

    if predictions_payload is None:
        predictions_payload = analyzed.get("top_predictions", [])

    if isinstance(predictions_payload, dict):
        predictions_payload = (
            predictions_payload.get("top_predictions")
            or predictions_payload.get("predictions")
            or predictions_payload.get("items")
            or []
        )

    if not isinstance(predictions_payload, list):
        return []

    normalized_predictions = []

    for item in predictions_payload:
        if not isinstance(item, dict):
            continue

        label = item.get("label") or item.get("class") or item.get("name")
        if not label:
            continue

        probability = item.get("probability", item.get("score", 0.0))
        try:
            probability = float(probability)
        except (TypeError, ValueError):
            probability = 0.0

        detected = item.get("detected", probability >= 0.5)
        if isinstance(detected, str):
            detected = detected.lower() in {"true", "1", "yes", "detected"}
        else:
            detected = bool(detected)

        normalized_predictions.append(
            {
                "label": str(label),
                "probability": probability,
                "detected": detected,
            }
        )

    return normalized_predictions


def ecg_to_response(record: EcgRecordEntity) -> EcgRecordResponse:
    return EcgRecordResponse(
        id=record.id,
        owner_user_id=record.owner_user_id,
        uploaded_by_user_id=record.uploaded_by_user_id,
        status=EcgStatus(record.status),
        storage_dir=record.storage_dir,
        digitized_csv_path=record.digitized_csv_path,
        digitization_metadata_path=record.digitization_metadata_path,
        completed_csv_path=record.completed_csv_path,
        completion_plot_path=record.completion_plot_path,
        analysis_json_path=record.analysis_json_path,
        analysis_report_path=record.analysis_report_path,
        top_predictions=parse_predictions(record.top_predictions_json),
        synthetic_image_path=record.synthetic_image_path,
        error_message=record.error_message,
        created_at=record.created_at.isoformat(),
        updated_at=record.updated_at.isoformat(),
    )

def conclusion_to_response(conclusion: DoctorConclusionEntity) -> DoctorConclusionResponse:
    return DoctorConclusionResponse(
        ecg_id=conclusion.ecg_id,
        doctor_id=conclusion.doctor_user_id,
        text=conclusion.text,
        created_at=conclusion.created_at.isoformat(),
        updated_at=conclusion.updated_at.isoformat(),
    )

def post_json(url: str, payload: dict, timeout: int = 1800) -> dict:
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="ignore")
        try:
            detail = json.loads(body)
        except Exception:
            detail = body
        raise HTTPException(status_code=exc.code, detail=detail) from exc


def remove_file_if_exists(path_value: str | None) -> None:
    if not path_value:
        return
    try:
        Path(path_value).unlink(missing_ok=True)
    except Exception:
        pass


def keep_only_needed_digitizer_outputs(digitized: dict) -> None:
    remove_file_if_exists(digitized.get("debug_png_path"))

    output_dir = digitized.get("output_dir")
    if output_dir:
        try:
            for png_path in Path(output_dir).rglob("*.png"):
                png_path.unlink(missing_ok=True)
        except Exception:
            pass


ECG_LEADS = ["I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"]


def parse_float_or_none(raw: str | None) -> float | None:
    if raw is None:
        return None

    value = raw.strip()
    if value == "":
        return None

    try:
        parsed = float(value)
    except ValueError:
        return None

    if math.isnan(parsed) or math.isinf(parsed):
        return None

    return parsed


def read_wide_ecg_csv(path_value: str, leads: list[str]) -> tuple[list[float] | None, dict[str, list[float | None]]]:
    path = Path(path_value)
    if not path.exists():
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"ECG CSV file not found: {path_value}",
        )

    import csv

    time_values: list[float] | None = None
    lead_values: dict[str, list[float | None]] = {lead: [] for lead in leads}

    with path.open("r", encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)

        if reader.fieldnames is None:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"ECG CSV file has no header: {path_value}",
            )

        has_time_column = "#" in reader.fieldnames

        if has_time_column:
            time_values = []

        for row in reader:
            if has_time_column:
                time_values.append(parse_float_or_none(row.get("#")) or 0.0)

            for lead in leads:
                lead_values[lead].append(parse_float_or_none(row.get(lead)))

    return time_values, lead_values


def infer_sampling_rate(time_values: list[float] | None, fallback: int = 500) -> int:
    if time_values is None or len(time_values) < 2:
        return fallback

    deltas = []
    previous = time_values[0]

    for current in time_values[1:50]:
        delta = current - previous
        previous = current
        if delta > 0:
            deltas.append(delta)

    if not deltas:
        return fallback

    average_delta = sum(deltas) / len(deltas)
    if average_delta <= 0:
        return fallback

    return int(round(1.0 / average_delta))


def build_signal_segments_for_lead(
    digitized_values: list[float | None],
    completed_values: list[float | None],
) -> tuple[str, list[EcgSignalSegmentResponse]]:
    segments: list[EcgSignalSegmentResponse] = []

    current_origin: str | None = None
    current_start_index = 0
    current_values: list[float] = []

    origins_seen: set[str] = set()

    digitized_count = len(digitized_values)
    completed_count = len(completed_values)

    for sample_index, completed_value in enumerate(completed_values):
        if completed_value is None:
            if current_origin is not None and current_values:
                segments.append(
                    EcgSignalSegmentResponse(
                        origin=current_origin,
                        start_sample_index=current_start_index,
                        voltage=current_values,
                    )
                )
            current_origin = None
            current_values = []
            continue

        digitized_value = None
        if digitized_count > 0 and completed_count > 0:
            digitized_index = int(sample_index * digitized_count / completed_count)
            digitized_index = min(digitized_index, digitized_count - 1)
            digitized_value = digitized_values[digitized_index]

        if digitized_value is not None:
            point_origin = "DIGITIZED"
            point_value = digitized_value
        else:
            point_origin = "RECONSTRUCTED"
            point_value = completed_value

        origins_seen.add(point_origin)

        if current_origin != point_origin:
            if current_origin is not None and current_values:
                segments.append(
                    EcgSignalSegmentResponse(
                        origin=current_origin,
                        start_sample_index=current_start_index,
                        voltage=current_values,
                    )
                )

            current_origin = point_origin
            current_start_index = sample_index
            current_values = [point_value]
        else:
            current_values.append(point_value)

    if current_origin is not None and current_values:
        segments.append(
            EcgSignalSegmentResponse(
                origin=current_origin,
                start_sample_index=current_start_index,
                voltage=current_values,
            )
        )

    if origins_seen == {"DIGITIZED"}:
        lead_origin = "DIGITIZED"
    elif origins_seen == {"RECONSTRUCTED"}:
        lead_origin = "RECONSTRUCTED"
    elif len(origins_seen) > 1:
        lead_origin = "MIXED"
    else:
        lead_origin = "RECONSTRUCTED"

    return lead_origin, segments


def build_ecg_signal_response(record: EcgRecordEntity) -> EcgSignalResponse:
    if record.completed_csv_path is None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Completed ECG signal is not available yet",
        )

    if record.digitized_csv_path is None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Digitized ECG signal is not available yet",
        )

    completed_time_values, completed_by_lead = read_wide_ecg_csv(record.completed_csv_path, ECG_LEADS)
    _, digitized_by_lead = read_wide_ecg_csv(record.digitized_csv_path, ECG_LEADS)

    sampling_rate = infer_sampling_rate(completed_time_values, fallback=500)
    sample_count = max((len(completed_by_lead[lead]) for lead in ECG_LEADS), default=0)
    duration_seconds = sample_count / sampling_rate if sampling_rate > 0 else 0.0

    response_leads: list[EcgSignalLeadResponse] = []

    for lead in ECG_LEADS:
        lead_origin, segments = build_signal_segments_for_lead(
            digitized_values=digitized_by_lead.get(lead, []),
            completed_values=completed_by_lead.get(lead, []),
        )

        response_leads.append(
            EcgSignalLeadResponse(
                lead=lead,
                origin=lead_origin,
                segments=segments,
            )
        )

    return EcgSignalResponse(
        ecg_id=record.id,
        sampling_rate=sampling_rate,
        duration_seconds=duration_seconds,
        leads=response_leads,
    )


def run_pipeline_for_record(
    record: EcgRecordEntity,
    source_path: str,
    db: Session,
) -> EcgRecordEntity:
    storage_dir = Path(record.storage_dir)
    digitizer_dir = storage_dir / "digitizer"
    completion_dir = storage_dir / "completion"
    analysis_dir = storage_dir / "analysis"

    digitizer_dir.mkdir(parents=True, exist_ok=True)
    completion_dir.mkdir(parents=True, exist_ok=True)
    analysis_dir.mkdir(parents=True, exist_ok=True)

    try:
        record.status = EcgStatus.DIGITIZING.value
        record.updated_at = utc_now()
        db.commit()

        digitized = post_json(
            DIGITIZER_URL + "/digitize",
            {
                "source_path": source_path,
                "output_dir": str(digitizer_dir),
            },
            timeout=1800,
        )

        keep_only_needed_digitizer_outputs(digitized)

        record.digitized_csv_path = digitized.get("digitized_csv_path")
        record.digitization_metadata_path = digitized.get("metadata_path")
        record.updated_at = utc_now()
        db.commit()

        record.status = EcgStatus.COMPLETING.value
        record.updated_at = utc_now()
        db.commit()

        completed = post_json(
            COMPLETION_URL + "/complete",
            {
                "digitized_csv_path": record.digitized_csv_path,
                "metadata_path": record.digitization_metadata_path,
                "output_dir": str(completion_dir),
                "layout": "from_digitizer",
            },
            timeout=1200,
        )

        record.completed_csv_path = completed.get("completed_csv_path")
        record.completion_plot_path = completed.get("plot_html_path")
        record.updated_at = utc_now()
        db.commit()

        record.status = EcgStatus.ANALYZING.value
        record.updated_at = utc_now()
        db.commit()

        analyzed = post_json(
            ANALYSIS_URL + "/analyze",
            {
                "csv_path": record.completed_csv_path,
                "output_dir": str(analysis_dir),
            },
            timeout=1200,
        )

        record.analysis_json_path = analyzed.get("result_path")
        record.analysis_report_path = analyzed.get("html_report_path")
        record.top_predictions_json = json.dumps(
            extract_top_predictions(analyzed),
            ensure_ascii=False,
        )
        remove_digitizer_pngs(digitizer_dir)

        record.status = EcgStatus.PROCESSED.value
        record.error_message = None
        record.updated_at = utc_now()
        db.commit()
        db.refresh(record)

        return record

    except Exception as exc:
        record.status = EcgStatus.ERROR.value
        record.error_message = str(exc)
        record.updated_at = utc_now()
        db.commit()
        db.refresh(record)
        raise


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "service": "rhythmlens-api",
        "timestamp": utc_now().isoformat(),
    }


@app.get("/")
def root() -> dict:
    return {
        "name": "RhythmLens API",
        "status": "running",
        "health": "/health",
    }


@app.post("/auth/register", response_model=AuthUserResponse)
def register(
    request: AuthSyncRequest,
    db: Annotated[Session, Depends(get_db)],
) -> AuthUserResponse:
    decoded_token = verify_id_token(request.id_token)
    firebase_uid = decoded_token["uid"]
    email = decoded_token.get("email")

    if not email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Firebase user email is empty",
        )

    existing_user = db.scalar(
        select(UserEntity).where(UserEntity.firebase_uid == firebase_uid)
    )
    if existing_user is not None:
        return user_to_response(existing_user)

    if request.full_name is None or request.full_name.strip() == "":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Full name is required",
        )

    if request.role is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Role is required",
        )

    now = utc_now()
    user = UserEntity(
        id=firebase_uid,
        firebase_uid=firebase_uid,
        email=email,
        full_name=request.full_name.strip(),
        role=request.role.value,
        created_at=now,
    )

    db.add(user)
    db.commit()
    db.refresh(user)

    if user.role == UserRole.PATIENT.value:
        get_or_create_patient_invite(user, db)

    return user_to_response(user)


@app.post("/auth/login", response_model=AuthUserResponse)
def login(
    request: AuthSyncRequest,
    db: Annotated[Session, Depends(get_db)],
) -> AuthUserResponse:
    decoded_token = verify_id_token(request.id_token)
    firebase_uid = decoded_token["uid"]

    user = db.scalar(
        select(UserEntity).where(UserEntity.firebase_uid == firebase_uid)
    )

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User profile is not registered",
        )

    if user.role == UserRole.PATIENT.value:
        get_or_create_patient_invite(user, db)

    return user_to_response(user)


@app.get("/auth/me", response_model=AuthUserResponse)
def me(
    current_user: Annotated[UserEntity, Depends(get_current_user)],
) -> AuthUserResponse:
    return user_to_response(current_user)


@app.get("/patients/me/invite-code", response_model=PatientInviteResponse)
def get_my_patient_invite_code(
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> PatientInviteResponse:
    require_role(current_user, UserRole.PATIENT)
    invite = get_or_create_patient_invite(current_user, db)
    return PatientInviteResponse(invite_code=invite.invite_code)


@app.post("/doctor/patients/attach", response_model=PatientResponse)
def attach_patient_to_doctor(
    request: AttachPatientRequest,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> PatientResponse:
    require_role(current_user, UserRole.DOCTOR)

    normalized_code = normalize_invite_code(request.invite_code)
    invite = db.scalar(
        select(PatientInviteEntity).where(PatientInviteEntity.invite_code == normalized_code)
    )

    if invite is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient invite code not found",
        )

    patient = db.get(UserEntity, invite.patient_user_id)
    if patient is None or patient.role != UserRole.PATIENT.value:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found",
        )

    link_id = doctor_patient_link_id(current_user.id, patient.id)
    link = db.get(DoctorPatientLinkEntity, link_id)
    if link is None:
        link = DoctorPatientLinkEntity(
            id=link_id,
            doctor_user_id=current_user.id,
            patient_user_id=patient.id,
            created_at=utc_now(),
        )
        db.add(link)
        db.commit()

    return patient_to_response(patient, db)


@app.get("/doctor/patients", response_model=list[PatientResponse])
def list_doctor_patients(
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[PatientResponse]:
    require_role(current_user, UserRole.DOCTOR)

    links = db.scalars(
        select(DoctorPatientLinkEntity)
        .where(DoctorPatientLinkEntity.doctor_user_id == current_user.id)
        .order_by(DoctorPatientLinkEntity.created_at.desc())
    ).all()

    patients: list[PatientResponse] = []
    for link in links:
        patient = db.get(UserEntity, link.patient_user_id)
        if patient is not None and patient.role == UserRole.PATIENT.value:
            patients.append(patient_to_response(patient, db))

    return patients


@app.post("/pipeline/process-image")
def process_image(request: PipelineRequest) -> dict:
    # Debug endpoint. It does not write ECG records to DB and is not intended for Android.
    job_id = uuid.uuid4().hex
    base_dir = Path(request.output_base_dir or f"/data/results/pipeline/{job_id}")
    digitizer_dir = base_dir / "digitizer"
    completion_dir = base_dir / "completion"
    analysis_dir = base_dir / "analysis"

    digitized = post_json(
        DIGITIZER_URL + "/digitize",
        {
            "source_path": request.source_path,
            "output_dir": str(digitizer_dir),
        },
    )

    keep_only_needed_digitizer_outputs(digitized)

    completed = post_json(
        COMPLETION_URL + "/complete",
        {
            "digitized_csv_path": digitized["digitized_csv_path"],
            "metadata_path": digitized.get("metadata_path"),
            "output_dir": str(completion_dir),
            "layout": "from_digitizer",
        },
    )

    analyzed = post_json(
        ANALYSIS_URL + "/analyze",
        {
            "csv_path": completed["completed_csv_path"],
            "output_dir": str(analysis_dir),
        },
    )

    remove_digitizer_pngs(digitizer_dir)
    digitized["debug_png_path"] = None
    digitized["debug_png_exists_after_cleanup"] = any(digitizer_dir.rglob("*.png"))

    return {
        "status": "ok",
        "job_id": job_id,
        "source_path": request.source_path,
        "digitized": digitized,
        "completed": completed,
        "analyzed": analyzed,
    }


@app.post("/ecg/upload", response_model=EcgRecordResponse)
async def upload_ecg(
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    file: UploadFile = File(...),
    owner_user_id: str | None = Form(default=None),
) -> EcgRecordResponse:
    suffix = Path(file.filename or "").suffix.lower()
    if suffix not in {".png", ".jpg", ".jpeg"}:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only PNG/JPG/JPEG ECG images are supported",
        )

    owner_id = resolve_ecg_owner_for_upload(
        current_user=current_user,
        owner_user_id=owner_user_id,
        db=db,
    )

    ecg_id = uuid.uuid4().hex
    now = utc_now()

    storage_dir = STORAGE_ROOT / "ecg" / ecg_id
    upload_work_dir = WORK_ROOT / "uploads" / ecg_id
    upload_work_dir.mkdir(parents=True, exist_ok=True)

    temp_source_path = upload_work_dir / f"source{suffix}"

    try:
        with temp_source_path.open("wb") as buffer:
            while True:
                chunk = await file.read(1024 * 1024)
                if not chunk:
                    break
                buffer.write(chunk)

        record = EcgRecordEntity(
            id=ecg_id,
            owner_user_id=owner_id,
            uploaded_by_user_id=current_user.id,
            status=EcgStatus.UPLOADED.value,
            storage_dir=str(storage_dir),
            digitized_csv_path=None,
            digitization_metadata_path=None,
            completed_csv_path=None,
            completion_plot_path=None,
            analysis_json_path=None,
            analysis_report_path=None,
            top_predictions_json=None,
            synthetic_image_path=None,
            error_message=None,
            created_at=now,
            updated_at=now,
        )

        db.add(record)
        db.commit()
        db.refresh(record)

        processed_record = run_pipeline_for_record(
            record=record,
            source_path=str(temp_source_path),
            db=db,
        )

        return ecg_to_response(processed_record)

    finally:
        # Source image is temporary and is not stored permanently.
        shutil.rmtree(upload_work_dir, ignore_errors=True)


@app.get("/ecg", response_model=list[EcgRecordResponse])
def list_ecg(
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[EcgRecordResponse]:
    if current_user.role == UserRole.PATIENT.value:
        records = db.scalars(
            select(EcgRecordEntity)
            .where(EcgRecordEntity.owner_user_id == current_user.id)
            .order_by(EcgRecordEntity.created_at.desc())
        ).all()
        return [ecg_to_response(record) for record in records]

    linked_patient_ids = get_linked_patient_ids(db, current_user.id)
    conditions = [EcgRecordEntity.uploaded_by_user_id == current_user.id]
    if linked_patient_ids:
        conditions.append(EcgRecordEntity.owner_user_id.in_(linked_patient_ids))

    records = db.scalars(
        select(EcgRecordEntity)
        .where(or_(*conditions))
        .order_by(EcgRecordEntity.created_at.desc())
    ).all()

    return [ecg_to_response(record) for record in records]


@app.get("/ecg/{ecg_id}", response_model=EcgRecordResponse)
def get_ecg(
    ecg_id: str,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EcgRecordResponse:
    record = db.get(EcgRecordEntity, ecg_id)
    if record is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="ECG record not found",
        )

    ensure_ecg_access(record, current_user, db)

    return ecg_to_response(record)


@app.delete("/ecg/{ecg_id}")
def delete_ecg(
    ecg_id: str,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> dict:
    record = db.get(EcgRecordEntity, ecg_id)
    if record is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="ECG record not found",
        )

    ensure_ecg_access(record, current_user, db)

    storage_dir = record.storage_dir

    db.query(EcgPredictionEntity).filter(
        EcgPredictionEntity.ecg_record_id == record.id
    ).delete(synchronize_session=False)

    db.delete(record)
    db.commit()

    shutil.rmtree(storage_dir, ignore_errors=True)

    return {
        "status": "deleted",
        "ecg_id": ecg_id,
    }


@app.get("/ecg/{ecg_id}/signal", response_model=EcgSignalResponse)
def get_ecg_signal(
    ecg_id: str,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EcgSignalResponse:
    record = db.get(EcgRecordEntity, ecg_id)
    if record is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="ECG record not found",
        )

    ensure_ecg_access(record, current_user, db)

    if record.status != EcgStatus.PROCESSED.value:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"ECG record is not processed yet: {record.status}",
        )

    return build_ecg_signal_response(record)

@app.get("/ecg/{ecg_id}/conclusion", response_model=DoctorConclusionResponse | None)
def get_doctor_conclusion(
    ecg_id: str,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> DoctorConclusionResponse | None:
    record = db.get(EcgRecordEntity, ecg_id)
    if record is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="ECG record not found",
        )

    ensure_ecg_access(record, current_user, db)

    conclusion = db.get(DoctorConclusionEntity, ecg_id)
    if conclusion is None:
        return None

    return conclusion_to_response(conclusion)


@app.put("/ecg/{ecg_id}/conclusion", response_model=DoctorConclusionResponse)
def save_doctor_conclusion(
    ecg_id: str,
    request: DoctorConclusionRequest,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> DoctorConclusionResponse:
    require_role(current_user, UserRole.DOCTOR)

    record = db.get(EcgRecordEntity, ecg_id)
    if record is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="ECG record not found",
        )

    ensure_ecg_access(record, current_user, db)

    now = utc_now()
    text = request.text.strip()
    conclusion = db.get(DoctorConclusionEntity, ecg_id)

    if conclusion is None:
        conclusion = DoctorConclusionEntity(
            ecg_id=ecg_id,
            doctor_user_id=current_user.id,
            text=text,
            created_at=now,
            updated_at=now,
        )
        db.add(conclusion)
    else:
        conclusion.doctor_user_id = current_user.id
        conclusion.text = text
        conclusion.updated_at = now

    db.commit()
    db.refresh(conclusion)

    return conclusion_to_response(conclusion)

@app.get("/ecg/{ecg_id}/analysis")
def get_ecg_analysis(
    ecg_id: str,
    current_user: Annotated[UserEntity, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> dict:
    record = db.get(EcgRecordEntity, ecg_id)
    if record is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="ECG record not found",
        )

    ensure_ecg_access(record, current_user, db)

    return {
        "ecg_id": record.id,
        "status": record.status,
        "analysis_json_path": record.analysis_json_path,
        "analysis_report_path": record.analysis_report_path,
        "top_predictions": parse_predictions(record.top_predictions_json),
    }


def remove_digitizer_pngs(output_dir: Path) -> None:
    removed = []
    errors = []

    for png_path in output_dir.rglob("*.png"):
        try:
            png_path.unlink(missing_ok=True)
            removed.append(str(png_path))
        except Exception as exc:
            errors.append(f"{png_path}: {exc}")

    if errors:
        raise RuntimeError("Failed to remove digitizer PNG files: " + "; ".join(errors))