from __future__ import annotations

import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import List, Tuple


class CaseNotFoundError(FileNotFoundError):
    pass


class DocumentNotFoundError(FileNotFoundError):
    pass


@dataclass
class DocumentMetadata:
    document_id: str
    name: str
    type: str
    size: int
    status: str


@dataclass
class CaseMetadata:
    case_id: str
    created_at: str
    documents: List[DocumentMetadata]

    @staticmethod
    def from_dict(payload: dict) -> "CaseMetadata":
        documents = [DocumentMetadata(**doc) for doc in payload.get("documents", [])]
        return CaseMetadata(
            case_id=payload["case_id"],
            created_at=payload["created_at"],
            documents=documents,
        )

    def to_dict(self) -> dict:
        return {
            "case_id": self.case_id,
            "created_at": self.created_at,
            "documents": [doc.__dict__ for doc in self.documents],
        }


class CaseViewer:
    def __init__(self, root: Path | None = None) -> None:
        default_root = Path(__file__).resolve().parents[2] / "storage"
        self.root = root or Path(os.environ.get("STORAGE_ROOT", default_root)).resolve()
        self.cases_dir = self.root / "cases"
        self.cases_dir.mkdir(parents=True, exist_ok=True)

    def _metadata_path(self, case_id: str) -> Path:
        return self.cases_dir / case_id / "metadata.json"

    def _load_case(self, case_id: str) -> CaseMetadata:
        path = self._metadata_path(case_id)
        if not path.exists():
            raise CaseNotFoundError(case_id)
        payload = json.loads(path.read_text(encoding="utf-8"))
        return CaseMetadata.from_dict(payload)

    def list_cases(self) -> List[CaseMetadata]:
        result: List[CaseMetadata] = []
        if not self.cases_dir.exists():
            return result
        for metadata_path in sorted(self.cases_dir.rglob("metadata.json")):
            payload = json.loads(metadata_path.read_text(encoding="utf-8"))
            result.append(CaseMetadata.from_dict(payload))
        result.sort(key=lambda case: case.created_at, reverse=True)
        return result

    def get_case(self, case_id: str) -> CaseMetadata:
        return self._load_case(case_id)

    def get_document_path(self, case_id: str, document_id: str) -> Path:
        case_dir = self.cases_dir / case_id / "documents"
        if not case_dir.exists():
            raise DocumentNotFoundError(document_id)
        for candidate in case_dir.iterdir():
            if candidate.name.startswith(document_id):
                return candidate
        raise DocumentNotFoundError(document_id)

    def get_document(self, case_id: str, document_id: str) -> Tuple[DocumentMetadata, bytes, str]:
        case = self._load_case(case_id)
        try:
            metadata = next(doc for doc in case.documents if doc.document_id == document_id)
        except StopIteration as exc:
            raise DocumentNotFoundError(document_id) from exc

        path = self.get_document_path(case_id, document_id)
        binary = path.read_bytes()
        mime = _guess_mime_type(path)
        return metadata, binary, mime


def _guess_mime_type(path: Path) -> str:
    lower = path.name.lower()
    if lower.endswith(".pdf"):
        return "application/pdf"
    if lower.endswith(".png"):
        return "image/png"
    if lower.endswith(".jpg") or lower.endswith(".jpeg"):
        return "image/jpeg"
    return "application/octet-stream"


__all__ = [
    "CaseNotFoundError",
    "DocumentNotFoundError",
    "CaseViewer",
    "CaseMetadata",
    "DocumentMetadata",
]
