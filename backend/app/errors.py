"""Единый формат ошибок: {"error":{"code":...,"reason":...,"request_id":...}}."""
import structlog
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy.exc import OperationalError
from starlette.exceptions import HTTPException as StarletteHTTPException

log = structlog.get_logger()

CODES = {
    400: "BAD_REQUEST",
    401: "UNAUTHORIZED",
    403: "FORBIDDEN",
    404: "NOT_FOUND",
    405: "METHOD_NOT_ALLOWED",
    426: "UPGRADE_REQUIRED",
    429: "TOO_MANY_REQUESTS",
    500: "INTERNAL_ERROR",
    503: "SERVICE_UNAVAILABLE",
}


class ApiError(Exception):
    """Ошибка, которую нужно вернуть клиенту в едином формате."""

    def __init__(self, status: int, reason: str | None = None, headers: dict[str, str] | None = None):
        self.status = status
        self.code = CODES.get(status, "ERROR")
        self.reason = reason
        self.headers = headers or {}


def error_response(request: Request, status: int, reason: str | None = None,
                   headers: dict[str, str] | None = None) -> JSONResponse:
    request_id = getattr(request.state, "request_id", None)
    error = {"code": CODES.get(status, "ERROR")}
    if reason:
        error["reason"] = reason
    error["request_id"] = request_id
    all_headers = dict(headers or {})
    if request_id:
        all_headers["X-Request-Id"] = request_id
    request.state.error_code = error["code"]
    request.state.error_reason = reason
    return JSONResponse(status_code=status, content={"error": error}, headers=all_headers)


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def api_error(request: Request, exc: ApiError):
        return error_response(request, exc.status, exc.reason, exc.headers)

    @app.exception_handler(RequestValidationError)
    async def validation_error(request: Request, exc: RequestValidationError):
        return error_response(request, 400, "VALIDATION_ERROR")

    @app.exception_handler(StarletteHTTPException)
    async def http_error(request: Request, exc: StarletteHTTPException):
        return error_response(request, exc.status_code)

    @app.exception_handler(OperationalError)
    async def db_unavailable(request: Request, exc: OperationalError):
        log.error("database_unavailable", error=type(exc).__name__)
        return error_response(request, 503, headers={"Retry-After": "30"})

    @app.exception_handler(Exception)
    async def unexpected_error(request: Request, exc: Exception):
        log.exception("unexpected_error")
        return error_response(request, 500)
