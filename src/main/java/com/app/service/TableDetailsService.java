package com.app.service;

import java.util.List;

import com.app.model.TableDetails;


public interface TableDetailsService {
    List<String> getAllTableNames();
    TableDetails addTable(TableDetails tableObj);
    String checkTableExists(String tableName);
    List<String> getTempTables();
    String getColumnNames(String tableName);
    void requireMasterTable(String tableName);
    void requireTempTable(String tableName);
    void requireRegisteredTable(String tableName);

    /** Resolve a UI/API table name to a row in table_details (handles _master/_temp/_history suffixes). */
    String resolveRegisteredTableName(String tableName);

    /** Physical history table name passed to history stored procedures (e.g. city_master_history). */
    String toHistoryProcedureTableName(String registeredTableName);
}
