package com.app.support;

import org.springframework.dao.DataAccessException;

public final class BulkUploadMessages {

    private BulkUploadMessages() {
    }

    public static String validationError(long csvRow, int columnIndex, String columnName, String cellValue) {
        String colLabel = columnName != null && !columnName.isBlank()
                ? columnName + " (column " + columnIndex + ")"
                : "column " + columnIndex;
        String valuePart = cellValue != null && !cellValue.isBlank()
                ? " Value: \"" + truncate(cellValue, 60) + "\"."
                : "";
        return "Row " + csvRow + ", " + colLabel + ": invalid characters or format." + valuePart
                + " Use only letters, numbers, spaces, and allowed symbols from the template.";
    }

    public static String fromDatabaseException(DataAccessException ex) {
        String raw = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        if (raw == null) {
            return "This row could not be saved. Check values against the template.";
        }
        String lower = raw.toLowerCase();
        if (lower.contains("duplicate")) {
            return "Duplicate row — a record with the same key may already exist.";
        }
        if (lower.contains("data too long")) {
            return "One or more values are too long for the column.";
        }
        if (lower.contains("cannot be null") || lower.contains("doesn't have a default")) {
            return "A required column is missing or empty.";
        }
        if (lower.contains("incorrect") && lower.contains("value")) {
            return "Invalid value for a column (wrong type or format).";
        }
        if (lower.contains("unknown column")) {
            return "CSV column does not match the table. Download the template and use the same headers.";
        }
        return "This row could not be saved. Check all column values match the template.";
    }

    public static String friendlyFileNameError() {
        return "File name is not allowed. Use only letters, numbers, spaces, dots, underscores, and parentheses.";
    }

    public static String friendlyEmptyFileError() {
        return "The file is empty. Add a header row and at least one data row, then try again.";
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }
}
