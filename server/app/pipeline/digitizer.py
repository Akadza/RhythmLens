from app.domain.enums import EcgLeadOrigin


LEADS = ["I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"]


def digitize_ecg(source_path: str) -> dict:
    points = []
    for index in range(500):
        time_ms = index * 20
        voltage = 0.4 if index % 50 == 0 else 0.02
        points.append({"time_ms": time_ms, "voltage_mv": voltage})

    return {
        "sampling_rate": 500,
        "duration_seconds": 10.0,
        "leads": {
            lead: points for lead in LEADS[:8]
        },
        "lead_origins": {
            lead: EcgLeadOrigin.DIGITIZED.value for lead in LEADS[:8]
        },
    }
