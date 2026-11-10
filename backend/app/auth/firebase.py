"""Проверка Firebase ID-токена (анализ экрана 01, раздел 4.4).

Выдачу и обновление токенов полностью выполняет Firebase; бэкенд только проверяет токен.
"""
import threading
from dataclasses import dataclass

import firebase_admin
from fastapi import Header
from firebase_admin import auth, credentials
from firebase_admin import exceptions as firebase_exceptions

from app.config import get_settings
from app.errors import ApiError

_app = None
_app_lock = threading.Lock()


@dataclass(frozen=True)
class FirebaseUser:
    uid: str
    email: str
    name: str | None


def _get_app():
    """Инициализация Firebase Admin SDK при первом обращении."""
    global _app
    with _app_lock:
        if _app is None:
            settings = get_settings()
            cred = credentials.Certificate(settings.firebase_credentials_file)
            _app = firebase_admin.initialize_app(
                cred,
                {"projectId": settings.firebase_project_id,
                 "httpTimeout": settings.firebase_timeout_seconds},
                name="quiet-friend",
            )
        return _app


def _verify(token: str, check_revoked: bool) -> dict:
    """Проверка подписи, срока, aud, iss; при check_revoked — запрос к Firebase."""
    return auth.verify_id_token(token, app=_get_app(), check_revoked=check_revoked)


def _authenticate(authorization: str | None, check_revoked: bool) -> FirebaseUser:
    if not authorization:
        raise ApiError(401, "TOKEN_MISSING")
    scheme, _, token = authorization.partition(" ")
    if scheme != "Bearer" or not token.strip():
        raise ApiError(401, "TOKEN_INVALID")

    try:
        claims = _verify(token.strip(), check_revoked)
    except auth.ExpiredIdTokenError:
        raise ApiError(401, "TOKEN_EXPIRED")
    except (auth.RevokedIdTokenError, auth.UserDisabledError):
        raise ApiError(401, "TOKEN_INVALID")
    except auth.CertificateFetchError:
        raise ApiError(503, headers={"Retry-After": "30"})
    except (auth.InvalidIdTokenError, ValueError):
        raise ApiError(401, "TOKEN_INVALID")
    except firebase_exceptions.FirebaseError:
        # Firebase недоступен при проверке отзыва — fail closed (архитектура, 3.5)
        raise ApiError(503, headers={"Retry-After": "30"})

    if claims.get("firebase", {}).get("sign_in_provider") != "google.com":
        raise ApiError(403, "SIGN_IN_PROVIDER_NOT_ALLOWED")
    if not claims.get("email"):
        raise ApiError(401, "TOKEN_INVALID")

    return FirebaseUser(uid=claims["uid"], email=claims["email"], name=claims.get("name"))


def firebase_user_checked_revoked(authorization: str | None = Header(default=None)) -> FirebaseUser:
    """Для входа и чувствительных операций: проверка с отзывом сессий."""
    return _authenticate(authorization, check_revoked=True)


def firebase_user(authorization: str | None = Header(default=None)) -> FirebaseUser:
    """Для остальных методов: проверка без сетевого запроса к Firebase."""
    return _authenticate(authorization, check_revoked=False)
