package com.app.service;

import java.util.List;

import com.app.model.TableDetails;


public interface TableDetailsService {
    List<String> getAllTableNames();
    TableDetails addTable(TableDetails tableObj);
    String checkTableExists(String tableName);
    List<String> getTempTables();
    String getColumnNames(String tableName);
}  
