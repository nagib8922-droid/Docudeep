package com.example.docudeep;

import java.text.Normalizer;
import java.util.Locale;

public enum DocumentType {
    PAYSLIP,
    TAX_NOTICE,
    EXPENSES;

    public static DocumentType fromLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Document type is required");
        }
        String normalized = Normalizer.normalize(label.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace("'", "_");
        return switch (normalized) {
            case "PAYSLIP", "BULLETIN_DE_PAIE" -> PAYSLIP;
            case "TAX_NOTICE", "AVIS_D_IMPOSITION", "AVIS_DIMPOSITION" -> TAX_NOTICE;
            case "EXPENSES", "CHARGES" -> EXPENSES;
            default -> throw new IllegalArgumentException("Unsupported document type: " + label);
        };
    }
}
