from __future__ import annotations

import base64
import json
import os
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, List
import imghdr
import uuid


ALLOWED_TYPES = {"bulletin_de_paie", "avis_d_imposition", "charges"}
MAX_FILES = 5
MAX_FILE_SIZE = 10 * 1024 * 1024
STATUS_STORED = "stored"


class ValidationError(ValueError):
    """Raised when an upload payload violates a business rule."""


@dataclass
class DocumentPayload:
    name: str
    declared_type: str
    raw: bytes

    @property
    def size(self) -> int:
        return len(self.raw)


@dataclass
class StoredDocument:
    document_id: str
    name: str
    type: str
    size: int
    status: str = STATUS_STORED


@dataclass
class CaseRecord:
    case_id: str
    created_at: str
    documents: List[StoredDocument]

    def to_dict(self) -> dict:
        return {
            "case_id": self.case_id,
            "created_at": self.created_at,
            "documents": [asdict(doc) for doc in self.documents],
        }


class CaseStorage:
    """Manage the on-disk representation of uploaded documents."""

    def __init__(self, root: Path | None = None) -> None:
        default_root = Path(__file__).resolve().parents[2] / "storage"
        self.root = root or Path(os.environ.get("STORAGE_ROOT", default_root)).resolve()
        self.cases_dir = self.root / "cases"
        self.cases_dir.mkdir(parents=True, exist_ok=True)

    @staticmethod
    def _sanitize_filename(name: str) -> str:
        base = name.strip().replace(" ", "_")
        return "".join(ch for ch in base if ch.isalnum() or ch in {"_", "-", "."}) or "document"

    def _detect_image(self, payload: DocumentPayload) -> None:
        detected = imghdr.what(None, h=payload.raw)
        if detected not in {"png", "jpeg"}:
            raise ValidationError(f"Le fichier {payload.name} semble corrompu ou illisible.")

    def _verify_payload(self, payload: DocumentPayload) -> None:
        if payload.declared_type not in ALLOWED_TYPES:
            raise ValidationError(f"Type de document invalide: {payload.declared_type}.")
        if payload.size == 0:
            raise ValidationError(f"{payload.name} est vide.")
        if payload.size > MAX_FILE_SIZE:
            raise ValidationError(f"{payload.name} dépasse la taille maximale de 10 Mo.")

        lower = payload.name.lower()
        if lower.endswith(".pdf"):
            if not payload.raw.startswith(b"%PDF"):
                raise ValidationError(f"{payload.name} n'est pas un PDF valide.")
        elif lower.endswith(".png") or lower.endswith(".jpg") or lower.endswith(".jpeg"):
            self._detect_image(payload)
        else:
            raise ValidationError(f"Format non supporté pour {payload.name}.")

    def _write_case(self, record: CaseRecord, payloads: Iterable[DocumentPayload]) -> CaseRecord:
        case_dir = self.cases_dir / record.case_id
        docs_dir = case_dir / "documents"
        docs_dir.mkdir(parents=True, exist_ok=True)

        for stored, payload in zip(record.documents, payloads):
            filename = f"{stored.document_id}_{self._sanitize_filename(payload.name)}"
            target = docs_dir / filename
            target.write_bytes(payload.raw)

        metadata_path = case_dir / "metadata.json"
        metadata_path.write_text(json.dumps(record.to_dict(), indent=2, ensure_ascii=False), encoding="utf-8")
        return record

    def create_case(self, payloads: Iterable[DocumentPayload]) -> CaseRecord:
        payloads = list(payloads)
        if not payloads:
            raise ValidationError("Aucun document fourni.")
        if len(payloads) > MAX_FILES:
            raise ValidationError("Nombre maximum de fichiers dépassé.")

        for payload in payloads:
            self._verify_payload(payload)

        case_id = uuid.uuid4().hex
        created_at = datetime.now(timezone.utc).isoformat()
        stored_documents = [
            StoredDocument(
                document_id=uuid.uuid4().hex,
                name=payload.name,
                type=payload.declared_type,
                size=payload.size,
            )
            for payload in payloads
        ]
        record = CaseRecord(case_id=case_id, created_at=created_at, documents=stored_documents)
        return self._write_case(record, payloads)

    def reset(self) -> None:
        if self.cases_dir.exists():
            for child in self.cases_dir.iterdir():
                if child.is_dir():
                    for inner in child.rglob("*"):
                        if inner.is_file():
                            inner.unlink()
                    for inner_dir in sorted((p for p in child.rglob("*") if p.is_dir()), reverse=True):
                        inner_dir.rmdir()
                    child.rmdir()
                else:
                    child.unlink()


def decode_documents(documents: Iterable[dict]) -> List[DocumentPayload]:
    payloads: List[DocumentPayload] = []
    for entry in documents:
        try:
            name = str(entry["name"])
            declared_type = str(entry["type"])
            raw = base64.b64decode(entry["content"], validate=True)
        except KeyError as exc:  # pragma: no cover - defensive
            raise ValidationError(f"Champ manquant: {exc.args[0]}") from exc
        except (TypeError, ValueError) as exc:
            raise ValidationError("Impossible de décoder le document envoyé.") from exc
        payloads.append(DocumentPayload(name=name, declared_type=declared_type, raw=raw))
    return payloads


__all__ = [
    "ALLOWED_TYPES",
    "MAX_FILES",
    "MAX_FILE_SIZE",
    "CaseStorage",
    "CaseRecord",
    "DocumentPayload",
    "StoredDocument",
    "ValidationError",
    "decode_documents",
]
