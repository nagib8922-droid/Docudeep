import base64
import json
import tempfile
import unittest
from pathlib import Path

from docudeep_upload import service


def sample_pdf() -> bytes:
    return b"%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n"


def sample_png() -> bytes:
    # PNG header followed by minimal IHDR chunk (width/height = 1)
    return (
        b"\x89PNG\r\n\x1a\n"
        b"\x00\x00\x00\rIHDR"
        b"\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00"
        b"\x90wS\xde"
        b"\x00\x00\x00\x00IEND\xaeB`\x82"
    )


class DecodeDocumentsTests(unittest.TestCase):
    def test_decode_documents(self):
        payloads = service.decode_documents(
            [
                {
                    "name": "test.pdf",
                    "type": "bulletin_de_paie",
                    "content": base64.b64encode(sample_pdf()).decode("ascii"),
                }
            ]
        )
        self.assertEqual(len(payloads), 1)
        self.assertEqual(payloads[0].name, "test.pdf")
        self.assertEqual(payloads[0].declared_type, "bulletin_de_paie")
        self.assertEqual(payloads[0].raw, sample_pdf())


class CaseStorageTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.storage = service.CaseStorage(root=Path(self.tmp.name))

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def test_create_case_success(self):
        payloads = [
            service.DocumentPayload("justificatif.pdf", "bulletin_de_paie", sample_pdf()),
            service.DocumentPayload("photo.png", "charges", sample_png()),
        ]
        record = self.storage.create_case(payloads)
        self.assertEqual(len(record.documents), 2)
        metadata_file = Path(self.tmp.name) / "cases" / record.case_id / "metadata.json"
        self.assertTrue(metadata_file.exists())
        metadata = json.loads(metadata_file.read_text(encoding="utf-8"))
        self.assertEqual(metadata["case_id"], record.case_id)
        self.assertEqual(len(metadata["documents"]), 2)

    def test_reject_too_many_files(self):
        payloads = [
            service.DocumentPayload(f"file{i}.pdf", "charges", sample_pdf())
            for i in range(service.MAX_FILES + 1)
        ]
        with self.assertRaises(service.ValidationError):
            self.storage.create_case(payloads)

    def test_reject_invalid_type(self):
        payloads = [service.DocumentPayload("file.pdf", "autre", sample_pdf())]
        with self.assertRaises(service.ValidationError):
            self.storage.create_case(payloads)


if __name__ == "__main__":  # pragma: no cover
    unittest.main()
