FROM python:3.10-slim

WORKDIR /service

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PIP_DEFAULT_TIMEOUT=300

RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc g++ git \
    && rm -rf /var/lib/apt/lists/*

COPY modules/analysis-service/requirements.txt /tmp/service-requirements.txt
RUN pip install --no-cache-dir -r /tmp/service-requirements.txt

RUN pip install --no-cache-dir \
    torch==2.4.0+cpu \
    --index-url https://download.pytorch.org/whl/cpu

COPY modules/analysis-service/ecgfounder-requirements.cpu.txt /tmp/ecgfounder-requirements.cpu.txt
RUN pip install --no-cache-dir -r /tmp/ecgfounder-requirements.cpu.txt

COPY modules/analysis-service/main.py .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8003"]
