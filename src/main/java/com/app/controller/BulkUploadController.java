package com.app.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.app.dto.BulkInsertResult;
import com.app.dto.UploadIssue;
import com.app.exception.HrmsApiException;
import com.app.service.ActivityLogService;
import com.app.service.GeneralService;
import com.app.support.BulkUploadMessages;
import com.app.utility.CommonUtils;
import com.app.utility.StringUtils;
import com.app.web.ApiResponses;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class BulkUploadController {

    private static final Logger logger = LogManager.getLogger(BulkUploadController.class);

    private final GeneralService genService;
    private final ActivityLogService activityLogService;

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
        final List<UploadIssue> issues = new ArrayList<>();

        UploadProgress(int totalRows) {
            this.totalRows = totalRows;
            this.processedRows = 0;
            this.errorCount = 0;
            this.status = "PROCESSING";
            this.message = "";
        }
    }

    @PostMapping("/addFileAsync")
    public ResponseEntity<?> addFileAsync(
            @RequestParam("file") MultipartFile formData,
            @RequestParam("tableName") String tableName,
            Principal principal) throws IOException {

        logger.info("addFileAsync table={} user={}", tableName, principal.getName());
        String username = principal.getName();
        String progressKey = username + "_" + tableName;

        validateFileName(formData.getOriginalFilename());

        String existingDataMessage = genService.checkDataExists(tableName);
        if (!"No data".equals(existingDataMessage)) {
            throw new HrmsApiException(existingDataMessage);
        }
        if (formData.isEmpty()) {
            throw new HrmsApiException(BulkUploadMessages.friendlyEmptyFileError());
        }

        List<String> data = parseCsv(formData);
        if (data.size() < 2) {
            throw new HrmsApiException(BulkUploadMessages.friendlyEmptyFileError());
        }

        int totalRows = data.size() - 1;
        logger.debug("CSV parsed. totalRows={}", totalRows);

        UploadProgress progress = new UploadProgress(totalRows);
        uploadProgressMap.put(progressKey, progress);

        executorService.submit(() -> processUploadAsync(data, tableName, username, progress, totalRows));

        Map<String, String> resultData = new HashMap<>();
        resultData.put("progressKey", progressKey);
        resultData.put("totalRows", String.valueOf(totalRows));
        return ApiResponses.ok("Upload started. Please check progress.", Collections.singletonList(resultData));
    }

    @GetMapping("/uploadProgress")
    public ResponseEntity<?> getUploadProgress(@RequestParam("progressKey") String progressKey) {
        UploadProgress progress = uploadProgressMap.get(progressKey);
        if (progress == null) {
            return ApiResponses.error(HttpStatus.NOT_FOUND, "No upload found for this key");
        }

        Map<String, Object> progressData = new HashMap<>();
        progressData.put("totalRows", progress.totalRows);
        progressData.put("processedRows", progress.processedRows);
        progressData.put("errorCount", progress.errorCount);
        progressData.put("status", progress.status);
        progressData.put("message", progress.message);
        progressData.put("issues", progress.issues);
        progressData.put("percentage",
                progress.totalRows > 0 ? (progress.processedRows * 100) / progress.totalRows : 0);

        if ("COMPLETED".equals(progress.status)
                || "ERROR".equals(progress.status)
                || "COMPLETED_WITH_ERRORS".equals(progress.status)) {
            scheduleProgressCleanup(progressKey);
        }

        return ApiResponses.ok("Progress retrieved", Collections.singletonList(progressData));
    }

    private void processUploadAsync(
            List<String> data,
            String tableName,
            String username,
            UploadProgress progress,
            int totalRows) {
        try {
            String headers = data.get(0).substring(0, data.get(0).length() - 1).replace("|", ",");
            List<String> rows = new ArrayList<>(totalRows);
            for (int i = 1; i < data.size(); i++) {
                rows.add(data.get(i).substring(0, data.get(i).length() - 1).replace("|", ","));
            }
            final int batchSize = 200;
            final int firstDataRow = 2;
            BulkInsertResult allErrors = new BulkInsertResult();
            for (int start = 0; start < rows.size(); start += batchSize) {
                int end = Math.min(start + batchSize, rows.size());
                List<String> chunk = rows.subList(start, end);
                BulkInsertResult batchResult = genService.addBulkTempDataBatch(
                        headers, chunk, tableName, firstDataRow + start);
                allErrors.merge(batchResult);
                progress.errorCount = allErrors.getErrorCount();
                progress.issues.clear();
                progress.issues.addAll(allErrors.getIssues());
                progress.processedRows = end;
                if (logger.isDebugEnabled()
                        && (progress.processedRows % 500 == 0 || progress.processedRows == totalRows)) {
                    logger.debug("Upload progress {}/{} for {}", progress.processedRows, totalRows, tableName);
                }
            }
            if (progress.errorCount > 0) {
                progress.status = "COMPLETED_WITH_ERRORS";
                int saved = totalRows - progress.errorCount;
                progress.message = saved > 0
                        ? "Upload finished: " + saved + " of " + totalRows + " rows saved. "
                                + progress.errorCount + " row(s) failed — see details below."
                        : "Upload failed: all " + progress.errorCount + " row(s) had errors — see details below.";
            } else {
                progress.status = "COMPLETED";
                progress.message = "Successfully uploaded " + totalRows + " rows.";
            }
            if ("COMPLETED".equals(progress.status) || "COMPLETED_WITH_ERRORS".equals(progress.status)) {
                int saved = totalRows - progress.errorCount;
                String detail = progress.errorCount > 0
                        ? saved + " of " + totalRows + " row(s), " + progress.errorCount + " failed"
                        : totalRows + " row(s)";
                activityLogService.recordUpload(tableName, username, detail);
            }
            logger.info("Bulk upload completed for {}", tableName);
        } catch (HrmsApiException ex) {
            logger.error("Upload rejected for {}", tableName, ex);
            progress.status = "ERROR";
            progress.message = ex.getMessage();
            progress.issues.clear();
            progress.issues.addAll(ex.getIssues());
        } catch (Exception ex) {
            logger.error("Error during async upload for {}", tableName, ex);
            progress.status = "ERROR";
            progress.message = "Upload stopped because of an unexpected error. Please try again or contact admin.";
        }
    }

    private void scheduleProgressCleanup(String progressKey) {
        Timer cleanupTimer = new Timer(true);
        cleanupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                uploadProgressMap.remove(progressKey);
                logger.debug("Cleaned up progress data for: {}", progressKey);
            }
        }, 30_000);
    }

    private void validateFileName(String fileName) {
        if (!StringUtils.isAlphanumericSpace(CommonUtils.customReplace(fileName, "_.()"))) {
            throw new HrmsApiException(BulkUploadMessages.friendlyFileNameError());
        }
    }

    private List<String> parseCsv(MultipartFile formData) throws IOException {
        List<String> data = new ArrayList<>();
        String[] headerNames = null;
        try (InputStream inputStream = formData.getInputStream();
                Reader reader = new BufferedReader(new InputStreamReader(inputStream));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            for (CSVRecord csvRecord : csvParser) {
                long rowNum = csvRecord.getRecordNumber();
                if (rowNum == 1) {
                    headerNames = new String[csvRecord.size()];
                    for (int i = 0; i < csvRecord.size(); i++) {
                        String h = csvRecord.get(i);
                        headerNames[i] = h != null && !h.isBlank() ? h.trim() : "Column " + (i + 1);
                    }
                }
                StringBuilder values = new StringBuilder();
                for (int i = 0; i < csvRecord.size(); i++) {
                    String cell = csvRecord.get(i);
                    if (cell != null
                            && !StringUtils.isAlphanumericSpace(CommonUtils.customReplace(cell, excelValidation))) {
                        String columnName = headerNames != null && i < headerNames.length
                                ? headerNames[i]
                                : "Column " + (i + 1);
                        throw new HrmsApiException(
                                BulkUploadMessages.validationError(rowNum, i + 1, columnName, cell),
                                (int) rowNum,
                                i + 1,
                                columnName,
                                cell);
                    }
                    if (cell == null) {
                        values.append("default");
                    } else {
                        values.append(cell).append("|");
                    }
                }
                data.add(values.toString());
            }
        } catch (HrmsApiException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("CSV parsing failed", ex);
            throw new HrmsApiException(
                    "Could not read the CSV file. Save as .csv (UTF-8), check commas and quotes, then try again.");
        }
        return data;
    }
}
