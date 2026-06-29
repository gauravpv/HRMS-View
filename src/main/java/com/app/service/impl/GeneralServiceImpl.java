package com.app.service.impl;

import java.sql.CallableStatement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedCaseInsensitiveMap;

import com.app.dto.BulkInsertResult;
import com.app.exception.HrmsApiException;
import com.app.support.BulkUploadMessages;
import com.app.support.DataMovementMessages;
import com.app.support.UserFacingMessages;
import com.app.support.DataMovementProcedures;
import com.app.model.RequestResponseLogDetails;
import com.app.service.GeneralService;
import com.app.service.RequestResponseLogDetailsService;
import com.app.service.TableDetailsService;
import com.app.support.JdbcResultHelper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GeneralServiceImpl implements GeneralService {

    private static final Logger logger = LogManager.getLogger(GeneralServiceImpl.class);
    private static final String PENDING_DATA_MSG = "Data is in pending action";
    private static final String TEMP_DATA_EXISTS_MSG = "Temp data exists! Please truncate data before inserting again";
    private final JdbcTemplate jdbcTemplate;
    private final RequestResponseLogDetailsService logService;
    private final TableDetailsService tabService;

    @Override
    public List<Object> getAllData(String tableName) {
        tabService.requireRegisteredTable(tableName);
        return callProcedure("{call SP_GET_TABLE_DATA(?)}", cs -> cs.setString(1, tableName));
    }

    @Override
    public List<Object> getHistoryData(String tableName, int historyId) {
        tabService.requireMasterTable(stripHistorySuffix(tableName));
        return callProcedure("{call SP_GET_HISTORY_TABLE_DATA(?,?)}", cs -> {
            cs.setString(1, tableName);
            cs.setInt(2, historyId);
        });
    }

    @Override
    public List<Object> getHistoryId(String tableName, String fromDate, String toDate) {
        tabService.requireMasterTable(stripHistorySuffix(tableName));
        return callProcedure("{call SP_GET_HISTORY_ID_DATE(?,?,?)}", cs -> {
            cs.setString(1, tableName);
            cs.setString(2, fromDate);
            cs.setString(3, toDate);
        });
    }

    private List<Object> callProcedure(String sql, StatementConfigurer configurer) {
        long started = System.currentTimeMillis();
        try {
            Map<String, Object> map = jdbcTemplate.call(connection -> {
                CallableStatement cs = connection.prepareCall(sql);
                configurer.configure(cs);
                return cs;
            }, new ArrayList<>());
            List<Object> rows = JdbcResultHelper.firstResultSet(map);
            long durationMs = System.currentTimeMillis() - started;
            if (durationMs > 3000) {
                logger.info("Slow procedure sql={} rows={} durationMs={}", sql, rows.size(), durationMs);
            } else if (logger.isDebugEnabled()) {
                logger.debug("Procedure ok sql={} rows={} durationMs={}", sql, rows.size(), durationMs);
            }
            return rows;
        } catch (DataAccessException ex) {
            logger.error("Database procedure failed sql={} durationMs={}", sql, System.currentTimeMillis() - started, ex);
            throw new HrmsApiException("Unable to retrieve data. Please contact admin.");
        }
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(CallableStatement cs) throws java.sql.SQLException;
    }

    @Override
    public String saveTempData(List<String> stringList, String userName) {
        if (stringList == null || stringList.size() < 5) {
            throw new HrmsApiException("Invalid data payload.");
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(stringList.get(4));
            String decodedString = new String(decodedBytes);
            String tempTable = decodedString.replace("master", "temp");
            tabService.requireTempTable(tempTable);

            Map<String, Object> check = jdbcTemplate.call(connection -> {
                CallableStatement cs = connection.prepareCall("{call SP_CHECK_DATA_EXISTS(?,?)}");
                cs.setString(1, tempTable);
                cs.setString(2, stringList.get(0));
                return cs;
            }, new ArrayList<>());

            if (JdbcResultHelper.countFromFirstRow(check) > 0) {
                throw new HrmsApiException(PENDING_DATA_MSG);
            }

            StringBuilder columnNames = new StringBuilder(stringList.get(2).substring(1));
            StringBuilder changedValues = new StringBuilder(stringList.get(3).substring(3));
            columnNames.append(", PREVIOUS_STATE, NEW_STATE, ACTION, UPDATED_BY");
            changedValues.append(", '")
                    .append(stringList.get(0))
                    .append("', '")
                    .append(stringList.get(1))
                    .append("', 'CREATED', '")
                    .append(userName)
                    .append("'");

            jdbcTemplate.call(connection -> {
                CallableStatement cs = connection.prepareCall("{call SP_INSERT_INTO_TABLE(?,?,?)}");
                cs.setString(1, columnNames.toString());
                cs.setString(2, changedValues.toString());
                cs.setString(3, tempTable);
                return cs;
            }, new ArrayList<>());

            RequestResponseLogDetails log = new RequestResponseLogDetails(
                    stringList.get(0), stringList.get(1), tempTable, userName, "CREATED");
            logService.saveLog(log);
            return "data inserted";
        } catch (IllegalArgumentException ex) {
            logger.warn("Invalid saveTempData payload", ex);
            throw new HrmsApiException("Invalid data payload.");
        } catch (DataAccessException ex) {
            logger.error("Failed to save temp data", ex);
            throw new HrmsApiException("Data not saved");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, List<LinkedCaseInsensitiveMap<?>>> getAllDataByAction(String action) {
        Map<String, List<LinkedCaseInsensitiveMap<?>>> tableDataMap = new HashMap<>();
        try {
            Map<String, Object> map = jdbcTemplate.call(connection -> {
                CallableStatement cs = connection.prepareCall("{call SP_GET_DATA_BY_ACTION(?)}");
                cs.setString(1, action);
                return cs;
            }, new ArrayList<>());

            int count = 1;
            String tabName = "";
            for (String key : map.keySet()) {
                if (key.contains("#result-set")) {
                    if (count % 2 != 0) {
                        ArrayList<LinkedCaseInsensitiveMap<?>> tableNameList =
                                (ArrayList<LinkedCaseInsensitiveMap<?>>) map.get(key);
                        if (!tableNameList.isEmpty()) {
                            LinkedCaseInsensitiveMap<?> mapTab = tableNameList.get(0);
                            tabName = (String) mapTab.get("tableName");
                            tableDataMap.put(tabName, null);
                        }
                    } else {
                        tableDataMap.put(tabName, (List<LinkedCaseInsensitiveMap<?>>) map.get(key));
                    }
                }
                count++;
            }
            return tableDataMap;
        } catch (DataAccessException ex) {
            logger.error("Failed to load data for action {}", action, ex);
            throw new HrmsApiException("Unable to retrieve pending data.");
        }
    }

    private static final int BULK_INSERT_BATCH_SIZE = 200;

    @Override
    public String addBulkTempData(String headers, String values, String tableName, String username) {
        tabService.requireTempTable(tableName);
        if (insertBulkRow(headers, values, tableName).isEmpty()) {
            return "Added bulk data!";
        }
        return "Error";
    }

    private java.util.Optional<String> insertBulkRow(String headers, String values, String tableName) {
        try {
            jdbcTemplate.call(connection -> {
                CallableStatement cs = connection.prepareCall("{call SP_ADD_BULK_DATA(?,?,?)}");
                cs.setString(1, headers);
                cs.setString(2, values);
                cs.setString(3, tableName);
                return cs;
            }, new ArrayList<>());
            return java.util.Optional.empty();
        } catch (DataAccessException ex) {
            logger.debug("Bulk insert failed for table {}: {}", tableName, ex.getMessage());
            return java.util.Optional.of(BulkUploadMessages.fromDatabaseException(ex));
        }
    }

    @Override
    public BulkInsertResult addBulkTempDataBatch(
            String headers,
            List<String> valueRows,
            String tableName,
            int firstDataRowNumber) {
        tabService.requireTempTable(tableName);
        BulkInsertResult result = new BulkInsertResult();
        if (valueRows == null || valueRows.isEmpty()) {
            return result;
        }
        for (int start = 0; start < valueRows.size(); start += BULK_INSERT_BATCH_SIZE) {
            int end = Math.min(start + BULK_INSERT_BATCH_SIZE, valueRows.size());
            List<String> chunk = valueRows.subList(start, end);
            try {
                StringBuilder valuesClause = new StringBuilder();
                for (int i = 0; i < chunk.size(); i++) {
                    if (i > 0) {
                        valuesClause.append(',');
                    }
                    valuesClause.append('(').append(chunk.get(i)).append(')');
                }
                String sql = "INSERT INTO " + tableName + " (" + headers + ") VALUES " + valuesClause;
                jdbcTemplate.execute(sql);
            } catch (DataAccessException ex) {
                logger.warn("Batch insert failed for {} (rows {}-{}), falling back to row-by-row",
                        tableName, start + firstDataRowNumber, end + firstDataRowNumber - 1, ex);
                for (int i = 0; i < chunk.size(); i++) {
                    int rowNumber = start + i + firstDataRowNumber;
                    insertBulkRow(headers, chunk.get(i), tableName)
                            .ifPresent(msg -> result.addIssue(rowNumber, msg));
                }
            }
        }
        return result;
    }

    @Override
    public String truncateTable(String tableName) {
        tabService.requireTempTable(tableName);
        try {
            jdbcTemplate.call(connection -> {
                CallableStatement cs = connection.prepareCall("{call SP_TRUNCATE_TABLE(?)}");
                cs.setString(1, tableName);
                return cs;
            }, new ArrayList<>());
            return "Table truncated!";
        } catch (DataAccessException ex) {
            logger.error("Truncate failed for table {}: {}", tableName, ex.getMessage(), ex);
            throw HrmsApiException.internal(
                    "Truncate failed for table " + tableName + ": " + ex.getMessage(),
                    UserFacingMessages.PRE_UPLOAD_FAILED);
        }
    }

    @Override
    public String moveToHistory(String tableName) {
        tabService.requireMasterTable(tableName);
        final String procedure = DataMovementProcedures.moveToHistory(tableName);
        executeProcedure(tableName, procedure);
        return "Moved to History!";
    }

    private void executeProcedure(String tableName, String procedure) {
        executeProcedure(tableName, procedure, UserFacingMessages.PRE_UPLOAD_FAILED);
    }

    private void executeProcedure(String tableName, String procedure, String clientMessage) {
        String callSql = "{call " + procedure + "()}";
        long started = System.currentTimeMillis();
        logger.info("Data movement calling procedure={} table={}", procedure, tableName);
        try {
            jdbcTemplate.call(connection -> connection.prepareCall(callSql), new ArrayList<>());
            logger.info("Data movement completed procedure={} table={} durationMs={}",
                    procedure, tableName, System.currentTimeMillis() - started);
        } catch (DataAccessException ex) {
            logger.error("Data movement failed procedure={} table={} durationMs={}: {}",
                    procedure, tableName, System.currentTimeMillis() - started,
                    DataMovementMessages.formatForLog(tableName, procedure, ex), ex);
            throw HrmsApiException.internal(
                    DataMovementMessages.formatForLog(tableName, procedure, ex),
                    clientMessage);
        }
    }

    @Override
    public String checkDataExists(String tableName) {
        tabService.requireTempTable(tableName);
        try {
            Map<String, Object> check = jdbcTemplate.call(connection -> {
                CallableStatement cs = connection.prepareCall("{call SP_CHECK_DATA_COUNT(?)}");
                cs.setString(1, tableName);
                return cs;
            }, new ArrayList<>());
            if (JdbcResultHelper.countFromFirstRow(check) > 0) {
                return TEMP_DATA_EXISTS_MSG;
            }
            return "No data";
        } catch (DataAccessException ex) {
            logger.error("Data existence check failed for {}: {}", tableName, ex.getMessage(), ex);
            throw HrmsApiException.internal(
                    "Data existence check failed for " + tableName + ": " + ex.getMessage(),
                    UserFacingMessages.OPERATION_FAILED);
        }
    }

    @Override
    public String masterDataMove(List<String> tableNames) {
        return runDataMovement(tableNames, true);
    }

    @Override
    public String mainDataMove(List<String> tableNames) {
        return runDataMovement(tableNames, false);
    }

    private String runDataMovement(List<String> tableNames, boolean includeHistoryStep) {
        if (tableNames == null || tableNames.isEmpty()) {
            throw new HrmsApiException("No tables selected for movement.");
        }
        for (String name : tableNames) {
            tabService.requireMasterTable(name);
            if (includeHistoryStep) {
                moveToHistory(name);
            }
            String procedure = includeHistoryStep
                    ? DataMovementProcedures.moveToMaster(name)
                    : DataMovementProcedures.moveToMain(name);
            executeProcedure(name, procedure, UserFacingMessages.DATA_MOVEMENT_FAILED);
        }
        return includeHistoryStep ? "Moved all data to Master" : "Moved all data to Main";
    }

    private static String stripHistorySuffix(String tableName) {
        if (tableName != null && tableName.endsWith("_history")) {
            return tableName.substring(0, tableName.length() - "_history".length());
        }
        return tableName;
    }
}
