"""Описание ошибок в документации /docs.

Схема совпадает с контрактом из анализа экрана 01: {"error":{"code","reason","request_id"}}.
Стандартный ответ FastAPI 422 убирается: наш код его не возвращает,
ошибки валидации приходят как 400 BAD_REQUEST / VALIDATION_ERROR.
"""
from fastapi import FastAPI
from fastapi.openapi.utils import get_openapi
from pydantic import BaseModel, Field

from app.errors import CODES


class ErrorBody(BaseModel):
    code: str = Field(description="Общий код ошибки, один на HTTP-статус")
    reason: str | None = Field(default=None, description="Уточнение причины; есть не у всех ошибок")
    request_id: str = Field(description="Идентификатор запроса для поиска в логах")


class ErrorResponse(BaseModel):
    error: ErrorBody


def _example(status: int, reason: str | None) -> dict:
    error = {"code": CODES[status]}
    if reason:
        error["reason"] = reason
    error["request_id"] = "3f1c9a2e-7b4d-4e8a-9c21-5d0f6b8e1a47"
    return {"error": error}


def error_responses(spec: dict[int, tuple[str, list[str | None]]]) -> dict:
    """spec: {статус: (описание, [возможные reason])} -> значение для responses= в роутере."""
    result = {}
    for status, (description, reasons) in spec.items():
        result[status] = {
            "model": ErrorResponse,
            "description": description,
            "content": {
                "application/json": {
                    "examples": {
                        (reason or CODES[status]): {"value": _example(status, reason)}
                        for reason in reasons
                    }
                }
            },
        }
    return result


def install_openapi(app: FastAPI) -> None:
    """Подменяет генерацию схемы: убирает ответ 422 и его схемы."""

    def custom_openapi() -> dict:
        if app.openapi_schema:
            return app.openapi_schema
        schema = get_openapi(title=app.title, version=app.version, routes=app.routes)
        for path in schema.get("paths", {}).values():
            for operation in path.values():
                operation.get("responses", {}).pop("422", None)
        schemas = schema.get("components", {}).get("schemas", {})
        schemas.pop("HTTPValidationError", None)
        schemas.pop("ValidationError", None)
        app.openapi_schema = schema
        return schema

    app.openapi = custom_openapi
