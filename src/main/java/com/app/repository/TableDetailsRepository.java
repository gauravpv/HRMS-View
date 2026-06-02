package com.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.app.model.TableDetails;

public interface TableDetailsRepository extends JpaRepository<TableDetails, Integer> {

    @Query("select u.tableName from TableDetails u where u.tableType = 'main_table' order by u.tableName")
    List<String> getAllTableNames();

    @Query("select count(u) from TableDetails u where u.tableName = ?1")
    Long countOfTable(String tableName);

    @Query("select u.tableName from TableDetails u where u.tableType = 'temp_table' order by u.tableName")
    List<String> getAllTempTable();

    @Query("select u.columnDetail from TableDetails u where u.tableName = ?1")
    String getColumnNames(String tableName);

    boolean existsByTableNameAndTableType(String tableName, String tableType);
}
