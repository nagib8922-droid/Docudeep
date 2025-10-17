import os
from pathlib import Path

import unittest

import app


class FrontendServiceTests(unittest.TestCase):
    def test_parse_args_defaults(self):
        args = app.parse_args(())
        self.assertEqual(args.host, app.DEFAULT_HOST)
        self.assertEqual(args.port, app.DEFAULT_PORT)

    def test_static_directory_exists(self):
        static_root = Path(__file__).resolve().parent.parent / "static"
        self.assertTrue((static_root / "index.html").exists())
        self.assertTrue((static_root / "app.js").exists())
        self.assertTrue((static_root / "styles.css").exists())


if __name__ == "__main__":  # pragma: no cover
    unittest.main()
