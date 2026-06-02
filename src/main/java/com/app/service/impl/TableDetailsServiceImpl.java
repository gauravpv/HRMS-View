package com.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.app.exception.HrmsApiException;
import com.app.model.TableDetails;
import com.app.repository.TableDetailsRepository;
import com.app.service.TableDetailsService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TableDetailsServiceImpl implements TableDetailsService {

    private final TableDetailsRepository tabRepo;

    @Override
    public List<String> getAllTableNames() {
        return tabRepo.getAllTableNames();
    }

    @Override
    public TableDetails addTable(TableDetails tableObj) {
        return tabRepo.save(tableObj);
    }

    @Override
    public String checkTableExists(String tableName) {
        Long count = tabRepo.countOfTable(tableName);
        return count > 0 ? "exists" : "add";
    }

    @Override
    public List<String> getTempTables() {
        return tabRepo.getAllTempTable();
    }

    @Override
    public String getColumnNames(String tableName) {
        requireRegisteredTable(tableName);
        return tabRepo.getColumnNames(tableName);
    }

    @Override
    public void requireMasterTable(String tableName) {
        if (tableName == null || tableName.isBlank()
                || !tabRepo.existsByTableNameAndTableType(tableName, "main_table")) {
            throw new HrmsApiException("Table is not registered for this operation.");
        }
    }

    @Override
    public void requireTempTable(String tableName) {
        if (tableName == null || tableName.isBlank()
                || !tabRepo.existsByTableNameAndTableType(tableName, "temp_table")) {
            throw new HrmsApiException("Table is not registered for this operation.");
        }
    }

    @Override
    public void requireRegisteredTable(String tableName) {
        if (tableName == null || tableName.isBlank() || tabRepo.countOfTable(tableName) == 0) {
            throw new HrmsApiException("Table is not registered for this operation.");
        }
    }
}
