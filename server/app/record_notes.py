from datetime import datetime
from typing import Annotated

from fastapi import Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy import DateTime, String, Text, select
from sqlalchemy.orm import Mapped, Session, mapped_column

from app.main import Base, EcgRecordEntity, UserEntity, UserRole, app, ensure_ecg_access, get_current_user, get_db, utc_now


class RecordNoteEntity(Base):
    __tablename__ = "record_notes"

    ecg_id: Mapped[str] = mapped_column(String, primary_key=True)
    author_user_id: Mapped[str] = mapped_column(String, index=True)
    text: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class RecordNoteRequest(BaseModel):
    text: str = Field(default="")


class RecordNoteResponse(BaseModel):
    ecg_id: str
    doctor_id: str
    text: str
    created_at: str
    updated_at: str
