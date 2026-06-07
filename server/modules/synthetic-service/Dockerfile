FROM python:3.12-slim

WORKDIR /service

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

COPY modules/synthetic-service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY modules/synthetic-service/main.py .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8004"]
