package com.app.serviceImpl;

import java.sql.CallableStatement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedCaseInsensitiveMap;

import com.app.model.RequestResponseLogDetails;
import com.app.repository.TableDetailsRepo;
import com.app.service.GeneralService;
import com.app.service.RequestResponseLogDetailsService;

@Service
@Transactional
public class GeneralServiceImpl implements GeneralService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private RequestResponseLogDetailsService logService;
    
    @Autowired
    private TableDetailsRepo tabRepo; 
    
    private static final Logger logger = LogManager.getLogger(GeneralServiceImpl.class);


    @Override
    public List<Object> getAllData(String tableName) {
        Map<String, Object> map = jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call SP_GET_TABLE_DATA(?)}");
            cs.setString(1, tableName);
            return cs;
        }, new ArrayList<>());
        
        if(map.get("#result-set-1") instanceof List) {
            return (List)(map.get("#result-set-1"));
        }
        return null;
    }
    
    @Override
    public List<Object> getHistoryData(String tableName, int HistoryId) {
        Map<String, Object> map = jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call SP_GET_HISTORY_TABLE_DATA(?,?)}");
            cs.setString(1, tableName);
            cs.setInt(2, HistoryId);
            return cs;
        }, new ArrayList<>());
        
        if(map.get("#result-set-1") instanceof List) {
            return (List)(map.get("#result-set-1"));
        }
        return null;
    }
    
    @Override
    public List<Object> getHistoryId(String tableName, String fromDate, String toDate) {
        Map<String, Object> map = jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call SP_GET_HISTORY_ID_DATE(?,?,?)}");
            cs.setString(1, tableName);
            cs.setString(2, fromDate);
            cs.setString(3, toDate);
            return cs;
        }, new ArrayList<>());
        
           if(map.get("#result-set-1") instanceof List) {
            return (List)(map.get("#result-set-1"));
        }
        return null;
    }
    
    public static String removeUnderscoreAndTitleCase(String input) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    sb.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
        
        return sb.toString();
    }
    
    //stringList would have [0]oldJson, [1]newJson, [2]column names, [3]changed values, [4]table name
    @Override
    public String saveTempData(List<String> stringList, String userName) throws Exception {
        //taking out the table name
        byte[] decodedBytes = Base64.getDecoder().decode(stringList.get(4));
        String decodedString = new String(decodedBytes);
        String tempTable = decodedString.replace("master", "temp");
        //first check if the data already exists as a newly CREATED row
        Map<String, Object> check = jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call SP_CHECK_DATA_EXISTS(?,?)}");
            cs.setString(1, tempTable);
            cs.setString(2, stringList.get(0));
            return cs;
        }, new ArrayList<>());
        ArrayList<?> list = (ArrayList<?>)check.get("#result-set-1");
        LinkedCaseInsensitiveMap<?> ins = (LinkedCaseInsensitiveMap< ?>)list.get(0);
        Long count = (Long)ins.get("COUNT(1)");
        if(count != null && count > 0) {
            return "exists";
        }
        StringBuilder  columnNames = new StringBuilder(stringList.get(2).substring(1));
        StringBuilder changedValues =  new StringBuilder(stringList.get(3).substring(3));
        //insert columns of for old, new json and state of the object
        columnNames.append(", PREVIOUS_STATE, NEW_STATE, ACTION, UPDATED_BY");
        //insert data for the columns for all above columns
        changedValues.append(", '" +stringList.get(0) + "', '" + stringList.get(1) + "', " + "'CREATED'" + ", '" + userName + "'");
        jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call SP_INSERT_INTO_TABLE(?,?,?)}");
            cs.setString(1, columnNames.toString());
            cs.setString(2, changedValues.toString());
            cs.setString(3, tempTable);
            return cs;
        }, new ArrayList<>());
        RequestResponseLogDetails log = new RequestResponseLogDetails(stringList.get(0), stringList.get(1), tempTable, userName, "CREATED");
        logService.saveLog(log);
        return "data inserted";
    }

    //getting all the created data for submission before approval
    @SuppressWarnings("unchecked")
    @Override
    public  Map<String, List<LinkedCaseInsensitiveMap<?>>> getAllDataByAction(String action) {
        Map<String, List<LinkedCaseInsensitiveMap<?>>> tableDataMap = new HashMap<>();
        Map<String, Object> map = jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call SP_GET_DATA_BY_ACTION(?)}");
            cs.setString(1, action);
            return cs;
        }, new ArrayList<>());
        int count = 1;
        String tabName = "";
        for(String key: map.keySet()) {
            //get all table names from where the data is being fetched
            if(key.contains("#result-set")) {
                if(count %2 != 0) {
                    ArrayList<LinkedCaseInsensitiveMap<?>> tableNameList = (ArrayList<LinkedCaseInsensitiveMap<?>>)map.get(key);
                    LinkedCaseInsensitiveMap<?> mapTab = (LinkedCaseInsensitiveMap<?>)tableNameList.get(0);
                    tabName = (String)mapTab.get("tableName");
                    tableDataMap.put(tabName,null);
                }else {
                    tableDataMap.put(tabName,(List<LinkedCaseInsensitiveMap<?>>) map.get(key));
                }
            }
           
            count++;
        }
        return tableDataMap;
    }

    @Override
    public String addBulkTempData(String headers, String values, String tableName, String username) {
        try{
            jdbcTemplate.call(connection -> {
                CallableStatement cs = connection.prepareCall("{call SP_ADD_BULK_DATA(?,?,?)}");
                cs.setString(1, headers);
                cs.setString(2, values);
                cs.setString(3, tableName);
                return cs;
            }, new ArrayList<>());
            
        }catch(Exception e) {
            logger.error(e);
            return "Error";
        }
//        String oldJson = values.substring(values.indexOf("'{"), values.indexOf("}'") + 2);
//        String newJson = values.substring(values.lastIndexOf("'{"), values.lastIndexOf("}'") + 2);
//        String action = values.substring(values.lastIndexOf(",")+1, values.length());
//        RequestResponseLogDetails log = new RequestResponseLogDetails(oldJson, newJson, tableName, username, action);
//        logService.saveLog(log);
        return "Added bulk data!";
    }

    @Override
    public String truncateTable(String tableName) {
        jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call SP_TRUNCATE_TABLE(?)}");
            cs.setString(1, tableName);
            return cs;
        }, new ArrayList<>());
        return "Table truncated!";
    }
    
    private String determineSP(String tableName) {
    	logger.info("SP_MOVE_" + tableName.toUpperCase()+ "_HISTORY");
        return "SP_MOVE_" + tableName.toUpperCase()+ "_HISTORY";
    }
    
    @Override
    public String moveToHistory(String tableName) {
    	final String SP = determineSP(tableName);
    	logger.info(SP);
        jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call " + SP + "}");
            return cs;
        }, new ArrayList<>());
        return "Moved to History!";
    }
    

    @Override
    public String checkDataExists(String tableName) {
        Map<String, Object> check = jdbcTemplate.call(connection -> {
            CallableStatement cs = connection.prepareCall("{call SP_CHECK_DATA_COUNT(?)}");
            cs.setString(1, tableName);
            return cs;
        }, new ArrayList<>());
        ArrayList<?> list = (ArrayList<?>)check.get("#result-set-1");
        LinkedCaseInsensitiveMap<?> ins = (LinkedCaseInsensitiveMap< ?>)list.get(0);
        Long count = (Long)ins.get("COUNT(1)");
        if(count != null && count > 0) {
            return "Temp data exists! Please truncate data before inserting again";
        }
        return "No data";
    }

    @Override
    public String masterDataMove(List<String> tableNames) {
        List<String> tabDetails = tabRepo.getAllTableNames();
        for(String name: tabDetails) {
            if(tableNames.contains(name)) {
            	moveToHistory(name); //Moving Master Data to History.
                String tab = name.replace("_master", "");
                jdbcTemplate.call(connection -> {
                    CallableStatement cs = connection.prepareCall("{call SP_MOVE_"+tab.toUpperCase()+"()}");
                    return cs;
                }, new ArrayList<>());
            }
        }
        return "Moved all data to Master";
    }
    
    @Override
    public String mainDataMove(List<String> tableNames) {
        List<String> tabDetails = tabRepo.getAllTableNames();
        for(String name: tabDetails) {
            if(tableNames.contains(name)) {
                String tab = name.replace("_master", "");
                jdbcTemplate.call(connection -> {
                    CallableStatement cs = connection.prepareCall("{call SP_MOVE_"+tab.toUpperCase()+"_MAIN()}");
                    return cs;
                }, new ArrayList<>());
            }
        }
        return "Moved all data to Main";
    }
    
    

}
