package com.app.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * Shapes stored-procedure rows for the Search Data grid: drops very large audit
 * JSON columns and caps row count so the browser receives a smaller payload.
 */
public final class SearchResultMapper {

    private static final Set<String> HEAVY_COLUMNS = Set.of("PREVIOUS_STATE", "NEW_STATE");
    static final int MAX_GRID_ROWS = 5000;

    private SearchResultMapper() {
    }

    public record GridResult(List<Object> rows, int totalRows, boolean truncated) {
    }

    public static GridResult forGrid(List<Object> rows) {
        if (rows == null || rows.isEmpty()) {
            return new GridResult(List.of(), 0, false);
        }
        int total = rows.size();
        int limit = Math.min(total, MAX_GRID_ROWS);
        List<Object> grid = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            grid.add(stripHeavyColumns(rows.get(i)));
        }
        return new GridResult(grid, total, total > limit);
    }

    static Object stripHeavyColumns(Object row) {
        if (!(row instanceof Map<?, ?> source)) {
            return row;
        }
        LinkedCaseInsensitiveMap<Object> copy = new LinkedCaseInsensitiveMap<>(source.size());
        source.forEach((key, value) -> {
            if (key != null && !isHeavyColumn(String.valueOf(key))) {
                copy.put(String.valueOf(key), value);
            }
        });
        return copy;
    }

    private static boolean isHeavyColumn(String columnName) {
        String normalized = columnName.replace("`", "").trim().toUpperCase(Locale.ROOT);
        return HEAVY_COLUMNS.contains(normalized);
    }
}
