"""Общие настройки тестов.

Тесты работают с отдельной базой (TEST_DATABASE_URL). Проверка токена Firebase подменяется
заглушкой, поэтому настоящие токены и доступ к Firebase не нужны.
"""
import os

if os.getenv("TEST_DATABASE_URL"):
    os.environ["DATABASE_URL"] = os.environ["TEST_DATABASE_URL"]
os.environ.setdefault("FIREBASE_PROJECT_ID", "test-project")
os.environ["MIN_APP_VERSION"] = "1.0.0"

import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402
from firebase_admin import auth  # noqa: E402
from firebase_admin import exceptions as firebase_exceptions  # noqa: E402
from sqlalchemy import text  # noqa: E402

import app.users.models  # noqa: E402,F401
from app import limits  # noqa: E402
from app.auth import firebase  # noqa: E402
from app.db import Base, engine  # noqa: E402
from app.main import create_app  # noqa: E402


def fake_verify(token: str, check_revoked: bool) -> dict:
    """Токены для тестов:
    valid:<uid> — корректный вход через Google; password:<uid> — вход не через Google;
    expired, invalid, revoked — ошибки токена; unavailable — Firebase недоступен.
    """
    if token.startswith("valid:") or token.startswith("password:"):
        provider, uid = token.split(":", 1)
        return {
            "uid": uid,
            "email": f"{uid}@example.com",
            "name": "Тестовый пользователь",
            "firebase": {"sign_in_provider": "google.com" if provider == "valid" else "password"},
            "_check_revoked": check_revoked,
        }
    if token == "expired":
        raise auth.ExpiredIdTokenError("expired", cause=None)
    if token == "revoked":
        raise auth.RevokedIdTokenError("revoked")
    if token == "unavailable":
        raise firebase_exceptions.UnavailableError("firebase is down")
    raise auth.InvalidIdTokenError("invalid")


@pytest.fixture(scope="session", autouse=True)
def database_schema():
    Base.metadata.drop_all(engine)
    Base.metadata.create_all(engine)
    yield
    Base.metadata.drop_all(engine)


@pytest.fixture(autouse=True)
def clean_state():
    with engine.begin() as conn:
        conn.execute(text("TRUNCATE users"))
    limits.storage.reset()
    yield


@pytest.fixture
def client(monkeypatch):
    calls = []

    def recording_verify(token, check_revoked):
        calls.append(check_revoked)
        return fake_verify(token, check_revoked)

    monkeypatch.setattr(firebase, "_verify", recording_verify)
    test_client = TestClient(create_app())
    test_client.verify_calls = calls
    return test_client
