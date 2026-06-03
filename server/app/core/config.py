import os


class Settings:
    database_url: str = os.environ.get(
        "DATABASE_URL",
        "postgresql+psycopg://rhythmlens:rhythmlens_password@postgres:5432/rhythmlens",
    )
    firebase_credentials_path: str | None = os.environ.get("FIREBASE_CREDENTIALS_PATH")
    upload_dir: str = os.environ.get("UPLOAD_DIR", "/data/uploads")


settings = Settings()
