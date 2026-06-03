from app.domain.enums import EcgLeadOrigin


RECONSTRUCTED_LEADS = ["V3", "V4", "V5", "V6"]


def complete_ecg_signal(signal: dict) -> dict:
    leads = signal.setdefault("leads", {})
    origins = signal.setdefault("lead_origins", {})
    base_points = next(iter(leads.values()), [])

    for lead in RECONSTRUCTED_LEADS:
        if lead not in leads:
            leads[lead] = base_points
            origins[lead] = EcgLeadOrigin.RECONSTRUCTED.value

    return signal
