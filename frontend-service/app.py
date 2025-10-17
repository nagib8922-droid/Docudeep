#!/usr/bin/env python3
"""Lightweight static file server for the DocuDeep frontend microservice."""
from __future__ import annotations

import argparse
import http.server
import os
import socketserver
from pathlib import Path
from typing import Tuple

DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8000


class FrontendRequestHandler(http.server.SimpleHTTPRequestHandler):
    """Serve files from the service's static directory with helpful defaults."""

    def __init__(self, *args, **kwargs):
        static_root = Path(__file__).resolve().parent / "static"
        super().__init__(*args, directory=str(static_root), **kwargs)

    def end_headers(self) -> None:  # pragma: no cover - exercised via integration tests
        # Encourage the browser to always fetch fresh assets while developing.
        self.send_header("Cache-Control", "no-store")
        super().end_headers()


class ThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True


def parse_args(argv: Tuple[str, ...] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default=os.environ.get("FRONTEND_HOST", DEFAULT_HOST))
    parser.add_argument(
        "--port", type=int, default=int(os.environ.get("FRONTEND_PORT", DEFAULT_PORT))
    )
    return parser.parse_args(argv)


def main(argv: Tuple[str, ...] | None = None) -> None:
    args = parse_args(argv)
    address = (args.host, args.port)
    with ThreadedTCPServer(address, FrontendRequestHandler) as httpd:
        print(f"DocuDeep frontend available on http://{args.host}:{args.port}")
        httpd.serve_forever()


if __name__ == "__main__":  # pragma: no cover - manual invocation entrypoint
    main()
