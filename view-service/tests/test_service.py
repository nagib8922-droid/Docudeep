import json
import tempfile
import unittest
from pathlib import Path

from docudeep_view import service


class CaseViewerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.cases_dir = self.root / "cases"
        self.cases_dir.mkdir(parents=True, exist_ok=True)

        self.case_id = "case123"
        self.doc_id = "doc456"
        case_dir = self.cases_dir / self.case_id
        docs_dir = case_dir / "documents"
        docs_dir.mkdir(parents=True, exist_ok=True)
        (docs_dir / f"{self.doc_id}_test.pdf").write_bytes(b"%PDF-1.4 test")

        metadata = {
            "case_id": self.case_id,
            "created_at": "2024-01-01T00:00:00Z",
            "documents": [
                {
                    "document_id": self.doc_id,
                    "name": "test.pdf",
                    "type": "charges",
                    "size": 12,
                    "status": "stored",
                }
            ],
        }
        (case_dir / "metadata.json").write_text(json.dumps(metadata), encoding="utf-8")
        self.viewer = service.CaseViewer(root=self.root)

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def test_list_cases(self):
        cases = self.viewer.list_cases()
        self.assertEqual(len(cases), 1)
        self.assertEqual(cases[0].case_id, self.case_id)

    def test_get_case(self):
        case = self.viewer.get_case(self.case_id)
        self.assertEqual(case.case_id, self.case_id)
        self.assertEqual(len(case.documents), 1)

    def test_get_document(self):
        metadata, binary, mime = self.viewer.get_document(self.case_id, self.doc_id)
        self.assertEqual(metadata.document_id, self.doc_id)
        self.assertEqual(binary, b"%PDF-1.4 test")
        self.assertEqual(mime, "application/pdf")

    def test_missing_case(self):
        with self.assertRaises(service.CaseNotFoundError):
            self.viewer.get_case("unknown")

    def test_missing_document(self):
        with self.assertRaises(service.DocumentNotFoundError):
            self.viewer.get_document(self.case_id, "other")


if __name__ == "__main__":  # pragma: no cover
    unittest.main()
