"""Тесты POST /api/v1/users по тест-кейсам из анализа экрана 01 (раздел 5.4)."""
import threading
import uuid

from sqlalchemy import func, select

from app.db import SessionLocal
from app.users import service
from app.users.models import User

URL = "/api/v1/users"


def headers(token: str | None = "valid:uid-1", version: str | None = "1.0.0") -> dict:
    result = {}
    if token is not None:
        result["Authorization"] = f"Bearer {token}"
    if version is not None:
        result["X-App-Version"] = version
    return result


def users_count() -> int:
    with SessionLocal() as db:
        return db.execute(select(func.count()).select_from(User)).scalar_one()


def assert_error(response, status: int, code: str, reason: str | None = None):
    assert response.status_code == status
    error = response.json()["error"]
    assert error["code"] == code
    assert error.get("reason") == reason
    assert error["request_id"] == response.headers["X-Request-Id"]


# 1
def test_first_login_creates_user(client):
    response = client.post(URL, json={"timezone": "Europe/Moscow"}, headers=headers())
    assert response.status_code == 201
    body = response.json()
    uuid.UUID(body["id"])
    assert body["documents_acceptance_required"] is True
    assert users_count() == 1
    assert client.verify_calls == [True], "при входе токен проверяется с отзывом сессий"


# 2
def test_repeat_login_returns_same_user_without_changes(client):
    first = client.post(URL, json={"timezone": "Europe/Moscow"}, headers=headers())
    second = client.post(URL, json={"timezone": "Asia/Yekaterinburg"}, headers=headers())
    assert second.status_code == 201
    assert second.json() == first.json()
    assert users_count() == 1
    with SessionLocal() as db:
        assert db.execute(select(User.timezone)).scalar_one() == "Europe/Moscow"


# 3
def test_concurrent_creation_makes_one_row():
    barrier = threading.Barrier(2)
    results = []

    def worker():
        with SessionLocal() as db:
            barrier.wait()
            results.append(service.get_or_create_user(
                db, firebase_uid="same-uid", email="a@example.com", name=None, timezone="UTC"))

    threads = [threading.Thread(target=worker) for _ in range(2)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    assert len(set(results)) == 1
    assert users_count() == 1


# 4
def test_missing_token(client):
    assert_error(client.post(URL, json={"timezone": "UTC"}, headers=headers(token=None)),
                 401, "UNAUTHORIZED", "TOKEN_MISSING")


# 5
def test_expired_token(client):
    assert_error(client.post(URL, json={"timezone": "UTC"}, headers=headers("expired")),
                 401, "UNAUTHORIZED", "TOKEN_EXPIRED")


# 6
def test_invalid_token(client):
    assert_error(client.post(URL, json={"timezone": "UTC"}, headers=headers("invalid")),
                 401, "UNAUTHORIZED", "TOKEN_INVALID")


# 7
def test_not_google_provider(client):
    assert_error(client.post(URL, json={"timezone": "UTC"}, headers=headers("password:uid-1")),
                 403, "FORBIDDEN", "SIGN_IN_PROVIDER_NOT_ALLOWED")


# 8
def test_invalid_timezone(client):
    for body in ({"timezone": "Moscow"}, {}, {"timezone": "Mars/Base"}):
        assert_error(client.post(URL, json=body, headers=headers()), 400, "BAD_REQUEST", "VALIDATION_ERROR")
    assert users_count() == 0


def test_missing_app_version(client):
    assert_error(client.post(URL, json={"timezone": "UTC"}, headers=headers(version=None)),
                 400, "BAD_REQUEST", "VALIDATION_ERROR")


# 9
def test_old_app_version(client):
    assert_error(client.post(URL, json={"timezone": "UTC"}, headers=headers(version="0.9.0")),
                 426, "UPGRADE_REQUIRED")


# 10
def test_revoked_token(client):
    assert_error(client.post(URL, json={"timezone": "UTC"}, headers=headers("revoked")),
                 401, "UNAUTHORIZED", "TOKEN_INVALID")


# 11
def test_firebase_unavailable(client):
    response = client.post(URL, json={"timezone": "UTC"}, headers=headers("unavailable"))
    assert_error(response, 503, "SERVICE_UNAVAILABLE")
    assert "Retry-After" in response.headers


def test_rate_limit(client):
    for _ in range(10):
        assert client.post(URL, json={"timezone": "UTC"}, headers=headers()).status_code == 201
    response = client.post(URL, json={"timezone": "UTC"}, headers=headers())
    assert_error(response, 429, "TOO_MANY_REQUESTS")
    assert int(response.headers["Retry-After"]) >= 1


def test_request_id_is_passed_through(client):
    request_id = str(uuid.uuid4())
    response = client.post(URL, json={"timezone": "UTC"},
                           headers={**headers(), "X-Request-Id": request_id})
    assert response.headers["X-Request-Id"] == request_id


def test_health(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
