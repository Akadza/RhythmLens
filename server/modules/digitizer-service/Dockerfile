FROM python:3.12-slim

WORKDIR /service

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PIP_DEFAULT_TIMEOUT=300

RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc g++ git libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY modules/digitizer-service/requirements.txt /tmp/service-requirements.txt
RUN pip install --no-cache-dir -r /tmp/service-requirements.txt

RUN pip install --no-cache-dir \
    torch==2.4.0+cpu \
    torchvision==0.19.0+cpu \
    torchaudio==2.4.0+cpu \
    --index-url https://download.pytorch.org/whl/cpu

COPY modules/repos/Open-ECG-Digitizer/requirements.txt /tmp/digitizer-requirements.txt
RUN grep -v -E '^(torch|torchvision|torchaudio)$' /tmp/digitizer-requirements.txt > /tmp/digitizer-requirements.no-main-torch.txt \
    && pip install --no-cache-dir -r /tmp/digitizer-requirements.no-main-torch.txt

COPY modules/digitizer-service/main.py .
COPY modules/digitizer-service/config ./config

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
