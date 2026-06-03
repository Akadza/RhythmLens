def analyze_ecg(signal: dict) -> dict:
    return {
        "probabilities": [
            {"title": "Норма", "code": "NORM", "probability": 72},
            {"title": "Фибрилляция предсердий", "code": "AF", "probability": 18},
            {"title": "ST-изменения", "code": "ST", "probability": 10},
        ],
        "model_version": "mock-analyzer-0.1",
    }
