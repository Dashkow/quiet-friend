"""Логика: создать пользователя или найти существующего (анализ экрана 01, раздел 4.5)."""
import uuid

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.orm import Session

from app.users.models import User


def get_or_create_user(db: Session, *, firebase_uid: str, email: str, name: str | None,
                       timezone: str) -> uuid.UUID:
    """Идемпотентно по firebase_uid: повторный вызов не создаёт дубликат и не меняет данные."""
    stmt = (
        insert(User)
        .values(firebase_uid=firebase_uid, email=email, name=name, timezone=timezone)
        .on_conflict_do_nothing(index_elements=[User.firebase_uid])
        .returning(User.id)
    )
    user_id = db.execute(stmt).scalar_one_or_none()
    if user_id is None:
        user_id = db.execute(select(User.id).where(User.firebase_uid == firebase_uid)).scalar_one()
    db.commit()
    return user_id


def documents_acceptance_required(db: Session, user_id: uuid.UUID) -> bool:
    # TODO: таблица принятых документов появится при анализе экрана «Принятие документов».
    # До этого считаем, что документы нужно принять всегда.
    return True
