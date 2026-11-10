"""Тело запроса и ответа POST /api/v1/users (анализ экрана 01, разделы 4.3 и 4.6)."""
import uuid
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import BaseModel, field_validator


class CreateUserRequest(BaseModel):
    timezone: str

    @field_validator("timezone")
    @classmethod
    def must_be_iana(cls, value: str) -> str:
        if "/" not in value and value != "UTC":
            raise ValueError("timezone must be an IANA name, e.g. Europe/Moscow")
        try:
            ZoneInfo(value)
        except (ZoneInfoNotFoundError, ValueError):
            raise ValueError("unknown timezone")
        return value


class CreateUserResponse(BaseModel):
    id: uuid.UUID
    documents_acceptance_required: bool
