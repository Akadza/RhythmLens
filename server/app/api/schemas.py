from pydantic import BaseModel, Field

from app.domain.enums import EcgStatus, UserRole


class AuthSyncRequest(BaseModel):
    id_token: str = Field(alias="id_token")
    full_name: str | None = Field(default=None, alias="full_name")
    role: UserRole | None = None


class UserResponse(BaseModel):
    id: str
    firebase_uid: str
    email: str
    full_name: str
    role: UserRole
    created_at: str


class PatientResponse(BaseModel):
    id: str
    user_id: str
    full_name: str
    invite_code: str
    doctor_id: str | None
    created_at: str


class LinkPatientRequest(BaseModel):
    invite_code: str


class EcgRecordResponse(BaseModel):
    id: str
    patient_id: str
    status: EcgStatus
    source_filename: str | None
    signal: dict | None
    analysis: dict | None
    error_message: str | None
    created_at: str
    updated_at: str


class SyntheticEcgResponse(BaseModel):
    signal: dict
