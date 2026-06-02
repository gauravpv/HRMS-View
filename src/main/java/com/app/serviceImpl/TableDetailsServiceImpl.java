package com.app.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.model.TableDetails;
import com.app.repository.TableDetailsRepo;
import com.app.service.TableDetailsService;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class TableDetailsServiceImpl implements TableDetailsService {

    @Autowired
    private TableDetailsRepo tabRepo;
    
    
    @Override
    public List<String> getAllTableNames() {
        return tabRepo.getAllTableNames();
    }


    @Override
    public TableDetails addTable(com.app.model.TableDetails tableObj) {
        return tabRepo.save(tableObj);
    }


    @Override
    public String checkTableExists(String tableName) {
        Long count = tabRepo.countOfTable(tableName);
        if(count > 0)
            return "exists";
        return "add";
    }


    @Override
    public List<String> getTempTables() {
        return tabRepo.getAllTempTable();
    }


    @Override
    public String getColumnNames(String tableName) {
        return tabRepo.getColumnNames(tableName);
    }
    
}