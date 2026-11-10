"""Ограничение частоты запросов (раздел 3.2 архитектуры).

Используется библиотека limits — основа slowapi. Проверка оформлена как зависимость FastAPI,
чтобы она выполнялась раньше проверки токена и не пропускала лишние запросы к Firebase.
Счётчики хранятся в памяти процесса: бэкенд работает в одном процессе Uvicorn.
"""
import time
from collections.abc import Callable

from fastapi import Request
from limits import parse
from limits.storage import MemoryStorage
from limits.strategies import FixedWindowRateLimiter

from app.config import get_settings
from app.errors import ApiError

storage = MemoryStorage()
_limiter = FixedWindowRateLimiter(storage)


def _client_ip(request: Request) -> str:
    return request.client.host if request.client else "unknown"


def ip_rate_limit(scope: str, get_limit: Callable[[], str]):
    """Зависимость: не больше N запросов с одного IP в рамках scope."""

    def dependency(request: Request) -> None:
        item = parse(get_limit())
        key = _client_ip(request)
        if not _limiter.hit(item, scope, key):
            reset_at, _remaining = _limiter.get_window_stats(item, scope, key)
            retry_after = max(1, int(reset_at - time.time()))
            raise ApiError(429, headers={"Retry-After": str(retry_after)})

    return dependency


global_ip_limit = ip_rate_limit("ip", lambda: get_settings().rate_limit_ip)
users_create_limit = ip_rate_limit("users_create", lambda: get_settings().rate_limit_users_create)
