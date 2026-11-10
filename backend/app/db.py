"""Подключение к PostgreSQL."""
from collections.abc import Iterator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import get_settings

engine = create_engine(get_settings().database_url, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


class Base(DeclarativeBase):
    """Базовый класс для всех таблиц."""


def get_db() -> Iterator[Session]:
    """Сессия базы на время одного запроса."""
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()
