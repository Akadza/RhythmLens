import math


LEADS = ["I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"]


def generate_synthetic_ecg(duration_seconds: int = 10, sampling_rate: int = 500) -> dict:
    point_count = duration_seconds * sampling_rate
    leads = {}

    for lead_index, lead in enumerate(LEADS):
        phase = lead_index * 0.12
        amplitude = 0.6 + lead_index * 0.02
        points = []
        for index in range(point_count):
            time_seconds = index / sampling_rate
            voltage = amplitude * math.sin(2.0 * math.pi * 1.2 * time_seconds + phase)
            points.append({
                "time_ms": int(time_seconds * 1000),
                "voltage_mv": round(voltage, 4),
            })
        leads[lead] = points

    return {
        "sampling_rate": sampling_rate,
        "duration_seconds": duration_seconds,
        "leads": leads,
    }
