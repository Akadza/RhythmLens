from enum import Enum


class UserRole(str, Enum):
    PATIENT = "PATIENT"
    DOCTOR = "DOCTOR"


class EcgStatus(str, Enum):
    DRAFT = "DRAFT"
    UPLOADING = "UPLOADING"
    DIGITIZING = "DIGITIZING"
    COMPLETING = "COMPLETING"
    ANALYZING = "ANALYZING"
    PROCESSED = "PROCESSED"
    ERROR = "ERROR"


class EcgLeadOrigin(str, Enum):
    DIGITIZED = "DIGITIZED"
    RECONSTRUCTED = "RECONSTRUCTED"
    MIXED = "MIXED"
