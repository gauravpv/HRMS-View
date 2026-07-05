package com.app.support;

import java.util.Locale;

/**
 * Resolves stored procedure names for data movement (must match MySQL {@code hrms_bre} definitions).
 */
public final class DataMovementProcedures {

    private static final String MASTER_SUFFIX = "_master";

    private DataMovementProcedures() {
    }

    /** e.g. minimum_wages_master → SP_MOVE_MINIMUM_WAGES_MAIN */
    public static String moveToMain(String masterTableName) {
        return "SP_MOVE_" + baseTableKey(masterTableName) + "_MAIN";
    }

    /** e.g. minimum_wages_master → SP_MOVE_MINIMUM_WAGES */
    public static String moveToMaster(String masterTableName) {
        return "SP_MOVE_" + baseTableKey(masterTableName);
    }

    /** e.g. band_master → SP_MOVE_BAND_MASTER_HISTORY (used when promoting temp → master). */
    public static String moveMasterToHistory(String masterTableName) {
        return "SP_MOVE_" + masterTableName.toUpperCase(Locale.ROOT) + "_HISTORY";
    }

    /** e.g. band_temp → SP_MOVE_BAND_TEMP_HISTORY (used before replacing temp upload data). */
    public static String moveTempToHistory(String tempTableName) {
        return "SP_MOVE_" + tempTableName.toUpperCase(Locale.ROOT) + "_HISTORY";
    }

    private static String baseTableKey(String masterTableName) {
        if (masterTableName == null || masterTableName.isBlank()) {
            return "";
        }
        String lower = masterTableName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(MASTER_SUFFIX)) {
            return masterTableName.substring(0, masterTableName.length() - MASTER_SUFFIX.length())
                    .toUpperCase(Locale.ROOT);
        }
        return masterTableName.toUpperCase(Locale.ROOT);
    }
}
