package com.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.io.*;
import java.security.Principal;
import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.app.utility.CommonUtils;
import com.app.utility.StringUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.app.dto.AjaxBody;
import com.app.dto.AjaxError;
import com.app.dto.StringListDto;
import com.app.service.GeneralService;
import com.app.service.TableDetailsService;

@RestController
@RequestMapping("/api/user")
public class UserRestController {
	
	@Autowired
    GeneralService genService;

    @Autowired
    TableDetailsService tabService;


    private static final Logger logger = LogManager.getLogger(UserRestController.class);
    
    @Value("${excel.validation}")
    private String excelValidation;

    @GetMapping("/searchTableData")
    public ResponseEntity<?> searchTableData(@RequestParam String tabName) {
        logger.info("Inside searchTableData");
        logger.info("Table Name: " + tabName);
        AjaxBody result = new AjaxBody();
        List<Object> list = genService.getAllData(tabName);
        result.setMsg("List retrieved");
        result.setResult(list);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/historyTableData")
    public ResponseEntity<?> historyTableData(@RequestParam String tabName, @RequestParam int historyId) {
        logger.info("Inside historyTableData");
        logger.info("Table Name: " + tabName);
        AjaxBody result = new AjaxBody();
        List<Object> list = genService.getHistoryData(tabName, historyId);
        result.setMsg("List retrieved");
        result.setResult(list);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/historyTableId")
    public ResponseEntity<?> historyTableId(@RequestParam String tabName, @RequestParam String fromDate, @RequestParam String toDate) {
        logger.info("Inside historyTableId");
        logger.info("Table Name: " + tabName);
        AjaxBody result = new AjaxBody();
        List<Object> list = genService.getHistoryId(tabName,fromDate, toDate);
        logger.info("Here "+list);
        result.setMsg("List retrieved");
        result.setResult(list);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/saveTempData")
    public ResponseEntity<?> saveTempData(StringListDto data, Principal principal) {
        logger.info("Inside saveTempData");
        logger.info("Data: " + data);
        String userName = principal.getName();
        String msg = "";
        try {
            msg = genService.saveTempData(data.getStringList(), userName);
        } catch (Exception e) {
            AjaxError err = new AjaxError();
            err.setErrorMsg("Data not saved");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            logger.error(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err);

        }
        if (msg.equals("exists")) {
            AjaxError err = new AjaxError();
            err.setErrorMsg("Data is in pending action");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            logger.error("Data is in pending action");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err);
        }
        AjaxBody result = new AjaxBody();
        result.setMsg(msg);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getAllCreatedData")
    public ResponseEntity<?> getAllCreatedData() {
        logger.info("Inside getAllCreatedData");
        Map<String, List<LinkedCaseInsensitiveMap<?>>> retreivedList = genService.getAllDataByAction("CREATED");
        // list structure would contain table name as key and data that is created as
        // the value of the key
        // for ex : internal_org_wide_temp=
        // [{PREVIOUS_STATE={"id":1,"band":"GB01","count":2670,"minpay":83596,"p10":104534.5,"p25":104534.5,"p30":105000,"p35":110549,"p50":115004,"p60":120000,"p65":120000,"p66":121000,"p70":124955.55,"p75":126820.13,"p90":135500,"maxpay":175434,"createdDate":"2022-12-20T02:27:17.000+00:00","lastUpdatedDate":"2022-12-20T02:27:17.000+00:00","status":0},
        // NEW_STATE={"band":"GB01","count":"2700","minpay":"83596","p10":"104534.5","p25":"104534.5","p30":"105000","p35":"110549","p50":"115004","p60":"120000","p65":"120000","p66":"121000","p70":"124955.55","p75":"126820.13","p90":"135500","maxpay":"175434","id":"","createdDate":"","lastUpdatedDate":"","status":0},
        // UPDATED_BY=admin, LAST_UPDATED_DATE=2023-03-06 07:56:41.0}]}
        List<Map<String, List<LinkedCaseInsensitiveMap<?>>>> list = new ArrayList<>();
        list.add(retreivedList);
        AjaxBody result = new AjaxBody();
        result.setResult(list);
        result.setMsg("Created data retreived");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/addFile")
    public ResponseEntity<?> addTableDetail(Model model, @RequestParam("file") MultipartFile formData,
            @RequestParam("tableName") String tableName, Principal principal)
            throws IOException {
        logger.info("Inside addFile");
        System.out.println("filename: " + formData.getOriginalFilename());
        if(!StringUtils.isAlphanumericSpace(CommonUtils.customReplace(formData.getOriginalFilename(), "_.()"))){
            AjaxError err = new AjaxError();
            err.setErrorMsg("Error in file name format");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            logger.error("Error in file name format");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err);
        }
        String test = genService.checkDataExists(tableName);
        String username = principal.getName();
        String msg = "";
       if(!test.equals("No data")) {
           AjaxError err = new AjaxError();
           err.setErrorMsg(test);
           err.setTime(new Timestamp(System.currentTimeMillis()));
           logger.error("Table has data! Please truncate before adding bulk");
           return ResponseEntity
                   .status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(err);
       }
        List<String> list = new ArrayList<>();
        if (formData.isEmpty()) {
            // send ajax error after this
            AjaxError err = new AjaxError();
            err.setErrorMsg("Excel empty");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            logger.error("Excel has no data! Add some data to excel");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err);
        } else {
            List<String> data = new ArrayList<>();
            try (
                    InputStream inputStream = formData.getInputStream();
                    Reader reader = new BufferedReader(new InputStreamReader(inputStream));
                    CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);) {
                for (CSVRecord csvRecord : csvParser) {
                    String values = "";
                    // Accessing Values by Column Index
                    for (int i = 0; i < csvRecord.size(); i++) {
                        if(csvRecord.get(i) != null ) {
                            if (!StringUtils.isAlphanumericSpace(CommonUtils.customReplace(csvRecord.get(i),excelValidation))) {
                                AjaxError err = new AjaxError();
                                logger.info("Error found for: " + csvRecord.get(i));
                                err.setErrorMsg("Data validation error for the field: " + csvRecord.get(i)+ " in Row: " + String.valueOf(csvRecord.getRecordNumber()) + " and Column: " + String.valueOf(i+1));
                                err.setTime(new Timestamp(System.currentTimeMillis()));
                                return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(err);
                            }
                        }
                        if (csvRecord.get(i) == null) {
                            values += "default";
                        } else {
                            values +=csvRecord.get(i) + "|";
                        }
                    }
                    data.add(values);
                }
            }
            String headers = data.get(0).substring(0, data.get(0).length()-1).replaceAll("\\|", ",");
            for(int i= 1; i < data.size(); i++) {
                String value = data.get(i).substring(0, data.get(i).length()-1).replaceAll("\\|", ",");
                logger.info("HEADER: "+headers + " VALUE: " + value + " Tablename: " + tableName);
                msg = genService.addBulkTempData(headers, value, tableName, username);
                logger.info(msg);
            }
        }
        if(msg.equals("Error")) {
            AjaxError err = new AjaxError();
            err.setErrorMsg("Error in data upload. Please contact admin");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err);
        }
        AjaxBody result = new AjaxBody();
        result.setResult(list);
        result.setMsg("Added data");
        return ResponseEntity.ok(result);
    }


    @GetMapping("/getColumns")
    public ResponseEntity<?> getAllColumns(String tabName) {
        logger.info("Inside getColumns");
        String columnNames = tabService.getColumnNames(tabName);
        AjaxBody result = new AjaxBody();
        List<String> columns = new ArrayList<>();
        columns.add(columnNames);
        result.setResult(columns);
        result.setMsg("columns retreived");
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/truncateTable")
    public ResponseEntity<?> truncateTable(String tableName) {
        logger.info("Inside truncateTable");
        logger.info("table: " + tableName);
        String msg = genService.truncateTable(tableName);
        AjaxBody result = new AjaxBody();
        result.setMsg(msg);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/moveToHistory")
    public ResponseEntity<?> moveToHistory(String tableName) {
        logger.info("Inside MoveToHistory");
        logger.info("table: " + tableName);
        String msg = genService.moveToHistory(tableName);
        AjaxBody result = new AjaxBody();
        result.setMsg(msg);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/masterDataMovement")
    public ResponseEntity<?> masterDataMovement(StringListDto tableNames) {
        logger.info("Inside masterDataMovement");
        logger.info("tables: " + tableNames);
        String msg = "";
        try {
            msg = genService.masterDataMove(tableNames.getStringList());
        }catch(Exception e) {
            AjaxError err = new AjaxError();
            err.setErrorMsg("Upload failed! Please contact admin");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            logger.error(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err);
        }
        AjaxBody result = new AjaxBody();
        result.setMsg(msg);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/mainDataMovement")
    public ResponseEntity<?> mainDataMovement(StringListDto tableNames) {
        logger.info("Inside mainDataMovement");
        logger.info("tables: " + tableNames);
        String msg = "";
        try {
            msg = genService.mainDataMove(tableNames.getStringList());
        }catch(Exception e) {
            AjaxError err = new AjaxError();
            err.setErrorMsg("Upload failed! Please contact admin");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            logger.error(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err);
        }
        AjaxBody result = new AjaxBody();
        result.setMsg(msg);
        return ResponseEntity.ok(result);
    }

}
