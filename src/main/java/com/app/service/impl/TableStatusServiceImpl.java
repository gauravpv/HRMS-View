package com.app.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.app.dto.TableStatusRow;
import com.app.service.TableDetailsService;
import com.app.service.TableStatusService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TableStatusServiceImpl implements TableStatusService {

    private static final Logger logger = LogManager.getLogger(TableStatusServiceImpl.class);
    private static final DateTimeFormatter LAST_UPDATED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final List<String> UPDATED_COLUMN_PRIORITY = List.of(
            "UPDATED_DATETIME",
            "LAST_UPDATED_DATE",
            "UPDATED_DATE",
            "LAST_UPDATED",
            "CREATED_DATETIME",
            "CREATED_DATE");

    private final JdbcTemplate jdbcTemplate;
    private final TableDetailsService tableDetailsService;

    @Value("${app.business-schema:hrms_bre}")
    private String businessSchema;

    @Override
    public List<TableStatusRow> getMainTableStatus() {
        List<String> tables = tableDetailsService.getAllTableNames();
        List<TableStatusRow> rows = new ArrayList<>(tables.size());
        for (String tableName : tables) {
            rows.add(fetchStatus(tableName));
        }
        logger.info("Table status loaded: {} tables from {}", rows.size(), businessSchema);
        return rows;
    }

    private TableStatusRow fetchStatus(String tableName) {
        String displayName = toDisplayName(tableName);
        try {
            tableDetailsService.requireMasterTable(tableName);
            requireSafeIdentifier(tableName);

            long count = queryRecordCount(tableName);
            String lastUpdated = queryLastUpdated(tableName);
            return new TableStatusRow(tableName, displayName, count, lastUpdated);
        } catch (Exception ex) {
            logger.warn("Table status failed for {}.{}: {}", businessSchema, tableName, ex.getMessage());
            return new TableStatusRow(tableName, displayName, -1L, null);
        }
    }

    private long queryRecordCount(String tableName) {
        String sql = "SELECT COUNT(*) AS record_count FROM " + qualifiedTable(tableName);
        Map<String, Object> row = jdbcTemplate.queryForMap(sql);
        return toLong(row.get("record_count"));
    }

    /**
     * Main tables in hrms_bre may use UPDATED_DATETIME or LAST_UPDATED_DATE (metadata in
     * decision_rules_hrmsbre often lists master/staging column names). Try each known name.
     */
    private String queryLastUpdated(String tableName) {
        for (String column : UPDATED_COLUMN_PRIORITY) {
            try {
                String sql = "SELECT MAX(`" + escapeIdentifier(column) + "`) AS last_updated FROM "
                        + qualifiedTable(tableName);
                Map<String, Object> row = jdbcTemplate.queryForMap(sql);
                Object value = row.get("last_updated");
                if (value != null) {
                    return formatLastUpdated(value);
                }
            } catch (DataAccessException ex) {
                logger.trace("Column {} not used for {}.{}", column, businessSchema, tableName);
            }
        }
        return null;
    }

    private String qualifiedTable(String tableName) {
        requireSafeIdentifier(businessSchema);
        return "`" + businessSchema + "`.`" + escapeIdentifier(tableName) + "`";
    }

    private static String toDisplayName(String tableName) {
        if (tableName == null) {
            return "";
        }
        String label = tableName;
        if (label.endsWith("_master")) {
            label = label.substring(0, label.length() - "_master".length());
        } else if (label.endsWith("_temp")) {
            label = label.substring(0, label.length() - "_temp".length());
        }
        return label.toUpperCase();
    }

    private static void requireSafeIdentifier(String name) {
        if (name == null || !SAFE_IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid database object name: " + name);
        }
    }

    private static String escapeIdentifier(String tableName) {
        return tableName.replace("`", "``");
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static String formatLastUpdated(Object value) {
        if (value == null) {
            return null;
        }
        LocalDateTime dateTime = null;
        if (value instanceof Timestamp timestamp) {
            dateTime = timestamp.toLocalDateTime();
        } else if (value instanceof LocalDateTime localDateTime) {
            dateTime = localDateTime;
        } else if (value instanceof java.util.Date date) {
            dateTime = new Timestamp(date.getTime()).toLocalDateTime();
        }
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(LAST_UPDATED_FORMAT);
    }
}
