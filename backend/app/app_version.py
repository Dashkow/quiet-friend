"""Проверка заголовка X-App-Version (минимальная поддерживаемая версия приложения)."""
import re

from fastapi import Header

from app.config import get_settings
from app.errors import ApiError

_VERSION = re.compile(r"^\d+(\.\d+){0,2}$")


def _parse(version: str) -> tuple[int, int, int]:
    parts = [int(p) for p in version.split(".")]
    while len(parts) < 3:
        parts.append(0)
    return parts[0], parts[1], parts[2]


def require_app_version(x_app_version: str | None = Header(default=None)) -> None:
    if not x_app_version or not _VERSION.match(x_app_version):
        raise ApiError(400, "VALIDATION_ERROR")
    if _parse(x_app_version) < _parse(get_settings().min_app_version):
        raise ApiError(426)
