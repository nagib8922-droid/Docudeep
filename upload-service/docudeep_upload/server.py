from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Tuple

from .service import CaseStorage, ValidationError, decode_documents

DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8001


class UploadHTTPRequestHandler(BaseHTTPRequestHandler):
    storage = CaseStorage()

    def _set_cors_headers(self) -> None:
        self.send_header("Access-Control-Allow-Origin", os.environ.get("FRONTEND_ORIGIN", "*"))
        self.send_header("Access-Control-Allow-Methods", "POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")

    def _send_json(self, status: HTTPStatus, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self._set_cors_headers()
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self) -> None:  # noqa: N802
        self.send_response(HTTPStatus.NO_CONTENT)
        self._set_cors_headers()
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_POST(self) -> None:  # noqa: N802
        if self.path == "/cases":
            return self._handle_create_case()
        if self.path == "/cases/reset":
            return self._handle_reset()
        self.send_error(HTTPStatus.NOT_FOUND, "Endpoint inconnu")

    def _handle_reset(self) -> None:
        self.storage.reset()
        self._send_json(HTTPStatus.OK, {"message": "Stockage nettoyé."})

    def _handle_create_case(self) -> None:
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length)
        try:
            payload = json.loads(raw.decode("utf-8")) if raw else {}
        except json.JSONDecodeError:
            return self._send_json(HTTPStatus.BAD_REQUEST, {"message": "Payload JSON invalide."})

        documents = payload.get("documents")
        if not isinstance(documents, list):
            return self._send_json(HTTPStatus.BAD_REQUEST, {"message": "Champ 'documents' invalide."})

        try:
            decoded = decode_documents(documents)
            record = self.storage.create_case(decoded)
        except ValidationError as exc:
            return self._send_json(HTTPStatus.BAD_REQUEST, {"message": str(exc)})

        response = record.to_dict()
        self._send_json(HTTPStatus.CREATED, response)


def parse_args(argv: Tuple[str, ...] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="DocuDeep upload microservice")
    parser.add_argument("--host", default=os.environ.get("UPLOAD_HOST", DEFAULT_HOST))
    parser.add_argument(
        "--port", type=int, default=int(os.environ.get("UPLOAD_PORT", DEFAULT_PORT))
    )
    parser.add_argument(
        "--storage", default=os.environ.get("STORAGE_ROOT")
    )
    return parser.parse_args(argv)


def main(argv: Tuple[str, ...] | None = None) -> None:
    args = parse_args(argv)
    if args.storage:
        UploadHTTPRequestHandler.storage = CaseStorage(Path(args.storage))
    server = ThreadingHTTPServer((args.host, args.port), UploadHTTPRequestHandler)
    print(f"Upload service prêt sur http://{args.host}:{args.port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:  # pragma: no cover - manual stop
        pass
    finally:
        server.server_close()


if __name__ == "__main__":  # pragma: no cover
    main()
