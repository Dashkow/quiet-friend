"""Точка входа бэкенда «Тихий друг»."""
from fastapi import APIRouter, Depends, FastAPI
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.config import get_settings
from app.db import get_db
from app.errors import register_error_handlers
from app.limits import global_ip_limit
from app.openapi import install_openapi
from app.logging import RequestContextMiddleware, configure_logging
from app.users.router import router as users_router


def create_app() -> FastAPI:
    settings = get_settings()
    configure_logging(settings.log_level)

    app = FastAPI(
        title="Тихий друг API",
        version="0.1.0",
        docs_url="/docs" if settings.docs_enabled else None,
        redoc_url=None,
        openapi_url="/openapi.json" if settings.docs_enabled else None,
    )
    app.add_middleware(RequestContextMiddleware)
    register_error_handlers(app)

    api = APIRouter(prefix="/api/v1", dependencies=[Depends(global_ip_limit)])
    api.include_router(users_router)
    app.include_router(api)
    install_openapi(app)

    @app.get("/health", tags=["service"], summary="Проверка, что сервис и база работают")
    def health(db: Session = Depends(get_db)) -> dict:
        db.execute(text("SELECT 1"))
        return {"status": "ok"}

    return app


app = create_app()
