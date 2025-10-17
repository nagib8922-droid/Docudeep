from __future__ import annotations

import argparse
import json
import os
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Tuple
from urllib.parse import urlparse

from .service import CaseNotFoundError, CaseViewer, DocumentNotFoundError

DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8002


class ViewHTTPRequestHandler(BaseHTTPRequestHandler):
    viewer = CaseViewer()

    def _set_cors_headers(self) -> None:
        self.send_header("Access-Control-Allow-Origin", os.environ.get("FRONTEND_ORIGIN", "*"))
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
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

    def do_GET(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        segments = [segment for segment in parsed.path.split("/") if segment]
        if not segments:
            return self._send_cases_list()
        if segments[0] != "cases":
            return self.send_error(HTTPStatus.NOT_FOUND, "Endpoint inconnu")
        if len(segments) == 1:
            return self._send_cases_list()
        if len(segments) == 2:
            return self._send_case(segments[1])
        if len(segments) == 4 and segments[2] == "documents":
            return self._send_document(segments[1], segments[3])
        return self.send_error(HTTPStatus.NOT_FOUND, "Endpoint inconnu")

    def _send_cases_list(self) -> None:
        cases = [case.to_dict() for case in self.viewer.list_cases()]
        self._send_json(HTTPStatus.OK, {"cases": cases})

    def _send_case(self, case_id: str) -> None:
        try:
            case = self.viewer.get_case(case_id)
        except CaseNotFoundError:
            return self._send_json(HTTPStatus.NOT_FOUND, {"message": "Dossier introuvable."})
        self._send_json(HTTPStatus.OK, case.to_dict())

    def _send_document(self, case_id: str, document_id: str) -> None:
        try:
            metadata, binary, mime = self.viewer.get_document(case_id, document_id)
        except CaseNotFoundError:
            return self._send_json(HTTPStatus.NOT_FOUND, {"message": "Dossier introuvable."})
        except DocumentNotFoundError:
            return self._send_json(HTTPStatus.NOT_FOUND, {"message": "Document introuvable."})

        self.send_response(HTTPStatus.OK)
        self._set_cors_headers()
        self.send_header("Content-Type", mime)
        self.send_header(
            "Content-Disposition",
            f"attachment; filename=\"{metadata.name}\"",
        )
        self.send_header("Content-Length", str(len(binary)))
        self.end_headers()
        self.wfile.write(binary)


def parse_args(argv: Tuple[str, ...] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="DocuDeep view microservice")
    parser.add_argument("--host", default=os.environ.get("VIEW_HOST", DEFAULT_HOST))
    parser.add_argument("--port", type=int, default=int(os.environ.get("VIEW_PORT", DEFAULT_PORT)))
    parser.add_argument("--storage", default=os.environ.get("STORAGE_ROOT"))
    return parser.parse_args(argv)


def main(argv: Tuple[str, ...] | None = None) -> None:
    args = parse_args(argv)
    if args.storage:
        ViewHTTPRequestHandler.viewer = CaseViewer(Path(args.storage))
    server = ThreadingHTTPServer((args.host, args.port), ViewHTTPRequestHandler)
    print(f"View service prêt sur http://{args.host}:{args.port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:  # pragma: no cover
        pass
    finally:
        server.server_close()


if __name__ == "__main__":  # pragma: no cover
    main()
