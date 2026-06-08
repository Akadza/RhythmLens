FROM python:3.12-slim

WORKDIR /service

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libgl1 \
        libglib2.0-0 \
        libgomp1 \
    && rm -rf /var/lib/apt/lists/*

COPY modules/synthetic-service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY modules/synthetic-service/main.py .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8004"]
