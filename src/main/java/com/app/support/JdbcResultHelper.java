package com.app.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedCaseInsensitiveMap;

@SuppressWarnings("unchecked")
public final class JdbcResultHelper {

    private static final String PRIMARY_RESULT_SET = "#result-set-1";

    private JdbcResultHelper() {
    }

    public static List<Object> firstResultSet(Map<String, Object> procedureResult) {
        if (procedureResult == null) {
            return Collections.emptyList();
        }
        Object result = procedureResult.get(PRIMARY_RESULT_SET);
        if (result instanceof List<?> list) {
            return (List<Object>) list;
        }
        return Collections.emptyList();
    }

    /** Returns the first non-empty result set (MySQL procedures often emit empty sets before SELECT output). */
    public static List<Object> firstNonEmptyResultSet(Map<String, Object> procedureResult) {
        if (procedureResult == null) {
            return Collections.emptyList();
        }
        List<Object> primary = firstResultSet(procedureResult);
        if (!primary.isEmpty()) {
            return primary;
        }
        for (Map.Entry<String, Object> entry : procedureResult.entrySet()) {
            if (!entry.getKey().contains("#result-set")) {
                continue;
            }
            if (entry.getValue() instanceof List<?> list && !list.isEmpty()) {
                return (List<Object>) list;
            }
        }
        return Collections.emptyList();
    }

    public static long countFromFirstRow(Map<String, Object> procedureResult) {
        List<Object> rows = firstResultSet(procedureResult);
        if (rows.isEmpty() || !(rows.get(0) instanceof LinkedCaseInsensitiveMap<?> row)) {
            return 0L;
        }
        Object count = row.get("COUNT(1)");
        if (count instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
