package com.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.app.model.TableDetails;

public interface TableDetailsRepo extends JpaRepository<TableDetails, Integer>{
    
    @Query("select u.tableName from TableDetails u where mainTableName = 'NA'")
    List<String> getAllTableNames();
    
    @Query("select count(u) from TableDetails u where tableName = ?1 ")
    Long countOfTable(String tableName);
    
    @Query("select u.tableName from TableDetails u where mainTableName <> 'NA'")
    List<String> getAllTempTable();
    
    @Query("select u.columnDetail from TableDetails u where u.tableName=?1")
    String getColumnNames(String tableName);
}
