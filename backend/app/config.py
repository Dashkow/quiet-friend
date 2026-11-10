"""Настройки приложения. Значения берутся из переменных окружения."""
from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str
    firebase_project_id: str
    firebase_credentials_file: str = "/run/secrets/firebase_admin_key"
    firebase_timeout_seconds: int = 3

    min_app_version: str = "1.0.0"

    # Ограничения частоты запросов (формат библиотеки limits)
    rate_limit_ip: str = "120/minute"
    rate_limit_users_create: str = "10/minute"

    log_level: str = "INFO"
    docs_enabled: bool = True


@lru_cache
def get_settings() -> Settings:
    return Settings()
