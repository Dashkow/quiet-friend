"""JSON-логи (ADR-0002) и request_id для каждого запроса.

В логи не пишутся токены, почта, имя и тела запросов.
"""
import logging
import time
import uuid

import structlog
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request


def configure_logging(level: str) -> None:
    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.processors.add_log_level,
            structlog.processors.TimeStamper(fmt="iso", utc=True),
            structlog.processors.dict_tracebacks,
            structlog.processors.JSONRenderer(ensure_ascii=False),
        ],
        wrapper_class=structlog.make_filtering_bound_logger(logging.getLevelName(level.upper())),
        logger_factory=structlog.PrintLoggerFactory(),
    )


def _request_id_from(header_value: str | None) -> str:
    """Берём X-Request-Id клиента, только если это корректный UUID; иначе создаём свой."""
    if header_value:
        try:
            return str(uuid.UUID(header_value))
        except ValueError:
            pass
    return str(uuid.uuid4())


class RequestContextMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        request_id = _request_id_from(request.headers.get("X-Request-Id"))
        request.state.request_id = request_id
        structlog.contextvars.clear_contextvars()
        structlog.contextvars.bind_contextvars(request_id=request_id)

        started = time.perf_counter()
        log = structlog.get_logger()
        try:
            response = await call_next(request)
        except Exception:
            log.info("request", method=request.method, path=request.url.path, status=500,
                     duration_ms=round((time.perf_counter() - started) * 1000, 1),
                     user_id=getattr(request.state, "user_id", None), code="INTERNAL_ERROR")
            raise

        response.headers["X-Request-Id"] = request_id
        log.info("request", method=request.method, path=request.url.path, status=response.status_code,
                 duration_ms=round((time.perf_counter() - started) * 1000, 1),
                 user_id=getattr(request.state, "user_id", None),
                 code=getattr(request.state, "error_code", None),
                 reason=getattr(request.state, "error_reason", None))
        return response
