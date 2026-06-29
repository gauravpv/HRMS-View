package com.app.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps stored-procedure snapshot rows to a stable JSON shape for the History page.
 */
public final class HistorySnapshotMapper {

    private static final String[] ID_FIELDS = {
            "HISTORY_ID", "history_id", "HID", "hid", "ID", "id"
    };

    private static final String[] DATE_FIELDS = {
            "DATE", "date", "HISTORY_DATE", "history_date", "CREATED_DATE", "created_date",
            "SNAPSHOT_DATE", "snapshot_date", "UPDATED_DATE", "updated_date", "MODIFIED_DATE", "modified_date"
    };

    private HistorySnapshotMapper() {
    }

    public static List<Map<String, Object>> normalize(List<Object> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Map<String, Object> snap = toSnapshot(row);
            if (snap != null) {
                out.add(snap);
            }
        }
        return out;
    }

    private static Map<String, Object> toSnapshot(Object row) {
        if (row instanceof Map<?, ?> map) {
            Object id = pickField(map, ID_FIELDS);
            Object date = pickField(map, DATE_FIELDS);
            if (id == null) {
                id = firstNonEmptyValue(map);
            }
            if (date == null) {
                date = secondNonEmptyValue(map, id);
            }
            if (id == null) {
                return null;
            }
            Map<String, Object> snap = new LinkedHashMap<>(2);
            snap.put("HISTORY_ID", id);
            snap.put("DATE", date != null ? date : "");
            return snap;
        }
        if (row == null) {
            return null;
        }
        Map<String, Object> snap = new LinkedHashMap<>(2);
        snap.put("HISTORY_ID", row);
        snap.put("DATE", "");
        return snap;
    }

    private static Object pickField(Map<?, ?> map, String[] names) {
        for (String name : names) {
            Object value = map.get(name);
            if (hasValue(value)) {
                return value;
            }
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = entry.getKey().toString();
            for (String name : names) {
                if (key.equalsIgnoreCase(name) && hasValue(entry.getValue())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static Object firstNonEmptyValue(Map<?, ?> map) {
        for (Object value : map.values()) {
            if (hasValue(value)) {
                return value;
            }
        }
        return null;
    }

    private static Object secondNonEmptyValue(Map<?, ?> map, Object first) {
        for (Object value : map.values()) {
            if (hasValue(value) && !valuesEqual(value, first)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        return true;
    }

    private static boolean valuesEqual(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.toString().equals(b.toString());
    }
}
