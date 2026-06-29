package com.app.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

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

    private static final String MASTER_SUFFIX = "_master";
    private static final String TEMP_SUFFIX = "_temp";
    private static final String HISTORY_SUFFIX = "_history";

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

    @Override
    public String resolveRegisteredTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new HrmsApiException("Table is not registered for this operation.");
        }
        String normalized = stripHistorySuffix(tableName.trim());
        for (String candidate : registrationCandidates(normalized)) {
            if (tabRepo.countOfTable(candidate) > 0) {
                return candidate;
            }
        }
        throw new HrmsApiException("Table is not registered for this operation.");
    }

    @Override
    public String toHistoryProcedureTableName(String registeredTableName) {
        String name = registeredTableName == null ? "" : registeredTableName.trim();
        if (name.isEmpty()) {
            return name;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(MASTER_SUFFIX)) {
            return name;
        }
        if (lower.endsWith(TEMP_SUFFIX)) {
            return name.substring(0, name.length() - TEMP_SUFFIX.length()) + MASTER_SUFFIX;
        }
        return name + MASTER_SUFFIX;
    }

    private static List<String> registrationCandidates(String base) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(base);
        String lower = base.toLowerCase(Locale.ROOT);
        if (lower.endsWith(MASTER_SUFFIX)) {
            candidates.add(base.substring(0, base.length() - MASTER_SUFFIX.length()));
        }
        if (lower.endsWith(TEMP_SUFFIX)) {
            candidates.add(base.substring(0, base.length() - TEMP_SUFFIX.length()) + MASTER_SUFFIX);
            candidates.add(base.substring(0, base.length() - TEMP_SUFFIX.length()));
        }
        if (!lower.endsWith(MASTER_SUFFIX)) {
            candidates.add(base + MASTER_SUFFIX);
        }
        return new ArrayList<>(candidates);
    }

    private static String stripHistorySuffix(String tableName) {
        if (tableName != null && tableName.toLowerCase(Locale.ROOT).endsWith(HISTORY_SUFFIX)) {
            return tableName.substring(0, tableName.length() - HISTORY_SUFFIX.length());
        }
        return tableName;
    }
}
