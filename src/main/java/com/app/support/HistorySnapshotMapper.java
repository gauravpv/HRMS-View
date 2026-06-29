package com.app.support;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.app.dto.HistorySnapshotDto;

/**
 * Maps stored-procedure snapshot rows to a stable JSON shape for the History page.
 */
public final class HistorySnapshotMapper {

    private static final String[] ID_FIELDS = {
            "HISTORY_ID", "history_id", "historyId", "HID", "hid", "ID", "id"
    };

    private static final String[] DATE_FIELDS = {
            "DATE", "date", "snapshotDate", "HISTORY_DATE", "history_date", "CREATED_DATE", "created_date",
            "SNAPSHOT_DATE", "snapshot_date", "UPDATED_DATE", "updated_date", "MODIFIED_DATE", "modified_date"
    };

    private HistorySnapshotMapper() {
    }

    public static List<HistorySnapshotDto> normalize(List<Object> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<HistorySnapshotDto> out = new ArrayList<>(rows.size());
        for (Object row : rows) {
            HistorySnapshotDto snap = toSnapshot(row);
            if (snap != null) {
                out.add(snap);
            }
        }
        return out;
    }

    private static HistorySnapshotDto toSnapshot(Object row) {
        if (row instanceof Map<?, ?> map) {
            return fromMap(map);
        }
        if (row instanceof List<?> list) {
            return fromSequence(list.isEmpty() ? null : list.get(0),
                    list.size() > 1 ? list.get(1) : null);
        }
        if (row != null && row.getClass().isArray()) {
            Object[] values = (Object[]) row;
            return fromSequence(values.length > 0 ? values[0] : null,
                    values.length > 1 ? values[1] : null);
        }
        return fromSequence(row, null);
    }

    private static HistorySnapshotDto fromMap(Map<?, ?> map) {
        Object id = pickField(map, ID_FIELDS);
        Object date = pickField(map, DATE_FIELDS);
        if (id == null) {
            id = firstNonEmptyValue(map);
        }
        if (date == null) {
            date = secondNonEmptyValue(map, id);
        }
        return fromSequence(id, date);
    }

    private static HistorySnapshotDto fromSequence(Object id, Object date) {
        if (!hasValue(id)) {
            return null;
        }
        HistorySnapshotDto dto = new HistorySnapshotDto();
        dto.setHistoryId(id);
        dto.setSnapshotDate(formatScalar(date));
        return dto;
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

    private static String formatScalar(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof TemporalAccessor temporal) {
            return temporal.toString();
        }
        if (value instanceof Date date) {
            return date.toInstant().toString();
        }
        return value.toString();
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
