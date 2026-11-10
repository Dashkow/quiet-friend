# Backend

Сервер приложения «Тихий друг»: Python 3.12, FastAPI, PostgreSQL.
Архитектура и стек — `docs/analysis/architecture.adoc`.

## Структура

| Путь | Что внутри |
| --- | --- |
| `app/main.py` | Точка входа: создание приложения, подключение модулей, `GET /health` |
| `app/config.py` | Настройки из переменных окружения |
| `app/db.py` | Подключение к PostgreSQL |
| `app/logging.py` | JSON-логи и `request_id` |
| `app/errors.py` | Единый формат ошибок |
| `app/limits.py` | Ограничение частоты запросов |
| `app/app_version.py` | Проверка `X-App-Version` |
| `app/auth/` | Проверка Firebase ID-токена |
| `app/users/` | `POST /api/v1/users` |
| `migrations/` | Миграции базы (Alembic) |
| `tests/` | Тесты |

## Запуск локально

Из корня репозитория:

1. `cp .env.example .env` и заполнить значения (пароль базы, путь к ключу Firebase).
2. `docker compose up --build` — поднимает PostgreSQL и бэкенд, применяет миграции.
3. Открыть http://localhost:8000/docs — документация API и ручные запросы.
4. Проверка работы: http://localhost:8000/health

После изменений в коде — снова `docker compose up --build`.

## Тесты

```
docker compose run --rm --build backend pytest -v
```

Тесты используют отдельную базу `<POSTGRES_DB>_test` и заглушку вместо Firebase.
