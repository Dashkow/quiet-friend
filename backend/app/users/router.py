"""Метод POST /api/v1/users (анализ экрана 01, раздел 4)."""
from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.app_version import require_app_version
from app.auth.firebase import FirebaseUser, firebase_user_checked_revoked
from app.db import get_db
from app.limits import users_create_limit
from app.openapi import error_responses
from app.users import service
from app.users.schemas import CreateUserRequest, CreateUserResponse

router = APIRouter(prefix="/users", tags=["users"])


@router.post(
    "",
    status_code=201,
    response_model=CreateUserResponse,
    dependencies=[Depends(users_create_limit)],
    summary="Регистрация или вход пользователя",
    responses=error_responses({
        400: ("Некорректный запрос: тело, часовой пояс или заголовок X-App-Version", ["VALIDATION_ERROR"]),
        401: ("Токен Firebase отсутствует, истёк или недействителен", ["TOKEN_MISSING", "TOKEN_EXPIRED", "TOKEN_INVALID"]),
        403: ("Вход выполнен не через Google", ["SIGN_IN_PROVIDER_NOT_ALLOWED"]),
        426: ("Версия приложения ниже минимальной, нужно обновиться", [None]),
        429: ("Слишком много запросов; заголовок Retry-After — через сколько секунд повторить", [None]),
        500: ("Внутренняя ошибка сервера", [None]),
        503: ("Firebase или база данных недоступны; заголовок Retry-After", [None]),
    }),
)
def create_user(
    request: Request,
    body: CreateUserRequest,
    user: FirebaseUser = Depends(firebase_user_checked_revoked),
    _version: None = Depends(require_app_version),
    db: Session = Depends(get_db),
) -> CreateUserResponse:
    user_id = service.get_or_create_user(
        db, firebase_uid=user.uid, email=user.email, name=user.name, timezone=body.timezone
    )
    request.state.user_id = str(user_id)
    return CreateUserResponse(
        id=user_id,
        documents_acceptance_required=service.documents_acceptance_required(db, user_id),
    )
