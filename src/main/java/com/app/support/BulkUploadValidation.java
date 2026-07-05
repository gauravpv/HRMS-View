package com.app.support;

import java.util.Locale;
import java.util.Set;

/**
 * Cell-level validation rules for CSV bulk upload.
 */
public final class BulkUploadValidation {

    private static final Set<String> SKIP_CELL_VALIDATION_COLUMNS = Set.of(
            "PREVIOUS_STATE",
            "NEW_STATE",
            "ACTION",
            "UPDATED_BY");

    private BulkUploadValidation() {
    }

    public static boolean shouldValidateCell(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return true;
        }
        String normalized = columnName.replace("`", "").trim().toUpperCase(Locale.ROOT);
        return !SKIP_CELL_VALIDATION_COLUMNS.contains(normalized);
    }
}
