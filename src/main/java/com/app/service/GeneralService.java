package com.app.service;

import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedCaseInsensitiveMap;

import com.app.dto.BulkInsertResult;

public interface GeneralService {
    List<Object> getAllData(String tableName);
    List<Object> getHistoryId(String tableName, String fromDate, String toDate);
    List<Object> getHistoryData(String tableName, int HistoryId);
    String saveTempData(List<String> stringList, String userName);
    Map<String, List<LinkedCaseInsensitiveMap<?>>> getAllDataByAction(String action);
    String addBulkTempData(String headers, String values, String tableName, String username);

    /** Inserts many rows using multi-row INSERT batches (faster than one SP call per row). */
    BulkInsertResult addBulkTempDataBatch(
            String headers, List<String> valueRows, String tableName, int firstDataRowNumber);
    String truncateTable(String tableName);
    String moveToHistory(String tableName);
    String moveTempToHistory(String tableName);
    String checkDataExists(String tableName);
    String masterDataMove(List<String> tableNames);
    String mainDataMove(List<String> tableNames);
}