package com.app.support;

import org.springframework.dao.DataAccessException;

public final class DataMovementMessages {

    private DataMovementMessages() {
    }

    /** Full detail for server logs only — never show this text in the UI. */
    public static String formatForLog(String tableName, String procedure, DataAccessException ex) {
        String raw = rootMessage(ex);
        String lower = raw != null ? raw.toLowerCase() : "";

        if (lower.contains("sp_error_log") && lower.contains("doesn't exist")) {
            return "Move failed for " + displayTable(tableName) + ". Table sp_error_log is missing. "
                    + "Ask your DBA to run db/create-sp-error-log.sql (creates hrms_bre.sp_error_log and "
                    + "infosec_bre.sp_error_log), then retry Move to Main.";
        }
        if (lower.contains("sp_error_log") && lower.contains("denied")) {
            return "Move failed for " + displayTable(tableName) + ". The database user cannot write to "
                    + "sp_error_log. Ask your DBA to run db/create-sp-error-log.sql, then retry Move to Main.";
        }
        if (lower.contains("denied")) {
            return "Move failed for " + displayTable(tableName) + ": database permission denied. " + shorten(raw);
        }
        if (lower.contains("unknown column")) {
            return "Move failed for " + displayTable(tableName) + ": column mismatch between master and main "
                    + "tables. " + shorten(raw);
        }
        if (lower.contains("doesn't exist")) {
            return "Move failed for " + displayTable(tableName) + ": a required table or procedure is missing. "
                    + shorten(raw);
        }
        return "Move failed for " + displayTable(tableName) + " (" + procedure + "). " + shorten(raw);
    }

    private static String displayTable(String tableName) {
        if (tableName != null && tableName.endsWith("_master")) {
            return tableName.substring(0, tableName.length() - "_master".length());
        }
        return tableName != null ? tableName : "table";
    }

    private static String rootMessage(DataAccessException ex) {
        if (ex.getMostSpecificCause() != null) {
            return ex.getMostSpecificCause().getMessage();
        }
        return ex.getMessage();
    }

    private static String shorten(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Check MySQL logs or contact your DBA.";
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 220) {
            return trimmed.substring(0, 217) + "...";
        }
        return trimmed;
    }
}
