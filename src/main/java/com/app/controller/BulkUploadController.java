package com.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.app.utility.CommonUtils;
import com.app.utility.StringUtils;
import com.app.dto.AjaxBody;
import com.app.dto.AjaxError;
import com.app.service.GeneralService;

import java.util.*;
import java.io.*;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/user")
public class BulkUploadController {

    @Autowired
    GeneralService genService;

    private static final Logger logger = LogManager.getLogger(BulkUploadController.class);
    
    @Value("${excel.validation}")
    private String excelValidation;

    private static final ConcurrentHashMap<String, UploadProgress> uploadProgressMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    static class UploadProgress {
        int totalRows;
        int processedRows;
        int errorCount;
        String status;
        String message;
        
        public UploadProgress(int totalRows) {
            this.totalRows = totalRows;
            this.processedRows = 0;
            this.errorCount = 0;
            this.status = "PROCESSING";
            this.message = "";
        }
    }

    @PostMapping("/addFileAsync")
    public ResponseEntity<?> addFileAsync(@RequestParam("file") MultipartFile formData,
            @RequestParam("tableName") String tableName, Principal principal) throws IOException {
        
        logger.info("Inside addFileAsync");
        String username = principal.getName();
        String progressKey = username + "_" + tableName;

        if(!StringUtils.isAlphanumericSpace(CommonUtils.customReplace(formData.getOriginalFilename(), "_.()"))){
            AjaxError err = new AjaxError();
            err.setErrorMsg("Error in file name format");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        String test = genService.checkDataExists(tableName);
        if(!test.equals("No data")) {
            AjaxError err = new AjaxError();
            err.setErrorMsg(test);
            err.setTime(new Timestamp(System.currentTimeMillis()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        if (formData.isEmpty()) {
            AjaxError err = new AjaxError();
            err.setErrorMsg("Excel empty");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        List<String> data = new ArrayList<>();
        try (InputStream inputStream = formData.getInputStream();
             Reader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            
            for (CSVRecord csvRecord : csvParser) {
                StringBuilder values = new StringBuilder();
                for (int i = 0; i < csvRecord.size(); i++) {
                    if(csvRecord.get(i) != null ) {
                        if (!StringUtils.isAlphanumericSpace(CommonUtils.customReplace(csvRecord.get(i), excelValidation))) {
                            AjaxError err = new AjaxError();
                            err.setErrorMsg("Data validation error for field: " + csvRecord.get(i) + 
                                " in Row: " + csvRecord.getRecordNumber() + " Column: " + (i+1));
                            err.setTime(new Timestamp(System.currentTimeMillis()));
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
                        }
                    }
                    if (csvRecord.get(i) == null) {
                        values.append("default");
                    } else {
                        values.append(csvRecord.get(i)).append("|");
                    }
                }
                data.add(values.toString());
            }
        } catch (Exception e) {
            logger.error("Error parsing CSV: ", e);
            AjaxError err = new AjaxError();
            err.setErrorMsg("Error parsing CSV file");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        int totalRows = data.size() - 1;
        logger.info("CSV parsed successfully. Total rows: " + totalRows);

        UploadProgress progress = new UploadProgress(totalRows);
        uploadProgressMap.put(progressKey, progress);

        executorService.submit(() -> {
            try {
                String headers = data.get(0).substring(0, data.get(0).length()-1).replaceAll("\\|", ",");
                
                for(int i = 1; i < data.size(); i++) {
                    String value = data.get(i).substring(0, data.get(i).length()-1).replaceAll("\\|", ",");
                    String result = genService.addBulkTempData(headers, value, tableName, username);
                    
                    if(result.equals("Error")) {
                        progress.errorCount++;
                    }
                    progress.processedRows++;
                    
                    if (progress.processedRows % 50 == 0) {
                        logger.info("Progress: " + progress.processedRows + "/" + totalRows + " rows");
                    }
                }
                
                if(progress.errorCount > 0) {
                    progress.status = "COMPLETED_WITH_ERRORS";
                    progress.message = "Upload completed with " + progress.errorCount + " errors";
                } else {
                    progress.status = "COMPLETED";
                    progress.message = "Successfully uploaded " + totalRows + " rows";
                }
                logger.info("Bulk upload completed for " + tableName);
                
            } catch (Exception e) {
                logger.error("Error during async upload: ", e);
                progress.status = "ERROR";
                progress.message = "Error during upload: " + e.getMessage();
            }
        });

        AjaxBody result = new AjaxBody();
        result.setMsg("Upload started. Please check progress.");
        Map<String, String> resultData = new HashMap<>();
        resultData.put("progressKey", progressKey);
        resultData.put("totalRows", String.valueOf(totalRows));
        result.setResult(Collections.singletonList(resultData));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/uploadProgress")
    public ResponseEntity<?> getUploadProgress(@RequestParam("progressKey") String progressKey) {
        UploadProgress progress = uploadProgressMap.get(progressKey);
        
        if (progress == null) {
            AjaxError err = new AjaxError();
            err.setErrorMsg("No upload found for this key");
            err.setTime(new Timestamp(System.currentTimeMillis()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        Map<String, Object> progressData = new HashMap<>();
        progressData.put("totalRows", progress.totalRows);
        progressData.put("processedRows", progress.processedRows);
        progressData.put("errorCount", progress.errorCount);
        progressData.put("status", progress.status);
        progressData.put("message", progress.message);
        progressData.put("percentage", progress.totalRows > 0 ? 
            (progress.processedRows * 100) / progress.totalRows : 0);

        AjaxBody result = new AjaxBody();
        result.setResult(Collections.singletonList(progressData));
        result.setMsg("Progress retrieved");

        if (progress.status.equals("COMPLETED") || progress.status.equals("ERROR") || 
            progress.status.equals("COMPLETED_WITH_ERRORS")) {
            Timer cleanupTimer = new Timer();
            cleanupTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    uploadProgressMap.remove(progressKey);
                    logger.info("Cleaned up progress data for: " + progressKey);
                }
            }, 30000);
        }

        return ResponseEntity.ok(result);
    }
}
