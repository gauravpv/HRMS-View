package com.app.service;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedCaseInsensitiveMap;

public interface GeneralService {
    List<Object> getAllData(String tableName);
    List<Object> getHistoryId(String tableName, String fromDate, String toDate);
    List<Object> getHistoryData(String tableName, int HistoryId);
    String saveTempData(List<String> stringList, String userName) throws Exception;
    Map<String, List<LinkedCaseInsensitiveMap<?>>> getAllDataByAction(String action);
    String addBulkTempData(String headers, String values, String tableName, String username);
    String truncateTable(String tableName);
    String moveToHistory(String tableName);
    String checkDataExists(String tableName);
    String masterDataMove(List<String> tableNames);
    String mainDataMove(List<String> tableNames);
}