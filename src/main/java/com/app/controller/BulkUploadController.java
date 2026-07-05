package com.app.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import com.app.support.BulkUploadValidation;
import com.app.support.UserFacingMessages;
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
    private static final ConcurrentHashMap<String, ChunkedUploadBuffer> chunkedUploadMap = new ConcurrentHashMap<>();
    private static final int MAX_CHUNK_BYTES = 8192;
    private static final int MAX_ENCODED_CHUNK_BYTES = 12_288;
    private static final int MAX_TOTAL_CHUNKS = 900;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    static class ChunkedUploadBuffer {
        final String tableName;
        final String fileName;
        final String username;
        final int totalChunks;
        final String[] chunks;
        int receivedCount;

        ChunkedUploadBuffer(String tableName, String fileName, String username, int totalChunks) {
            this.tableName = tableName;
            this.fileName = fileName;
            this.username = username;
            this.totalChunks = totalChunks;
            this.chunks = new String[totalChunks];
        }

        boolean isComplete() {
            return receivedCount == totalChunks;
        }
    }

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

    /**
     * Accepts CSV content in small chunks (multipart preferred) so each request stays within
     * Akamai WAF body inspection limits (rule 3000180).
     */
    @PostMapping(value = "/addFileChunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addFileChunkMultipart(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("tableName") String tableName,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "encoding", defaultValue = "plain") String encoding,
            @RequestParam("chunk") String chunkData,
            Principal principal) throws IOException {
        return handleAddFileChunk(
                uploadId, chunkIndex, totalChunks, tableName, fileName, encoding, chunkData, principal);
    }

    @PostMapping(value = "/addFileChunk", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> addFileChunkPlain(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("tableName") String tableName,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "encoding", defaultValue = "plain") String encoding,
            @RequestBody(required = false) String chunkData,
            Principal principal) throws IOException {
        return handleAddFileChunk(
                uploadId, chunkIndex, totalChunks, tableName, fileName, encoding, chunkData, principal);
    }

    private ResponseEntity<?> handleAddFileChunk(
            String uploadId,
            int chunkIndex,
            int totalChunks,
            String tableName,
            String fileName,
            String encoding,
            String chunkData,
            Principal principal) throws IOException {

        String username = principal.getName();
        validateChunkedUploadParams(uploadId, chunkIndex, totalChunks, tableName, fileName);
        if (chunkData == null || chunkData.isBlank()) {
            throw HrmsApiException.internal(
                    "Upload chunk missing uploadId=" + uploadId + " chunkIndex=" + chunkIndex + "/"
                            + totalChunks + " user=" + username
                            + " — request body may have been removed by WAF/proxy",
                    UserFacingMessages.UPLOAD_FAILED);
        }
        String decodedChunk = decodeChunkBody(chunkData, encoding);

        String bufferKey = username + "_" + uploadId;
        ChunkedUploadBuffer buffer = chunkedUploadMap.computeIfAbsent(bufferKey, key -> {
            validateFileName(fileName);
            String existingDataMessage = genService.checkDataExists(tableName);
            if (!"No data".equals(existingDataMessage)) {
                throw new HrmsApiException(existingDataMessage);
            }
            return new ChunkedUploadBuffer(tableName, fileName, username, totalChunks);
        });

        if (!buffer.tableName.equals(tableName) || !buffer.username.equals(username)) {
            chunkedUploadMap.remove(bufferKey);
            throw new HrmsApiException("Upload session mismatch. Please start the upload again.");
        }
        if (buffer.chunks[chunkIndex] != null) {
            throw new HrmsApiException("Duplicate chunk received. Please start the upload again.");
        }

        buffer.chunks[chunkIndex] = decodedChunk;
        buffer.receivedCount++;

        if (!buffer.isComplete()) {
            Map<String, String> ack = new HashMap<>();
            ack.put("uploadId", uploadId);
            ack.put("chunkIndex", String.valueOf(chunkIndex));
            ack.put("receivedChunks", String.valueOf(buffer.receivedCount));
            return ApiResponses.ok("Chunk received", Collections.singletonList(ack));
        }

        chunkedUploadMap.remove(bufferKey);
        String csvContent = String.join("", buffer.chunks);
        if (csvContent.isBlank()) {
            throw new HrmsApiException(BulkUploadMessages.friendlyEmptyFileError());
        }

        logger.info("addFileChunk complete table={} user={} chunks={}", tableName, username, totalChunks);
        return startAsyncUploadFromCsv(csvContent, tableName, username);
    }

    private ResponseEntity<?> startAsyncUploadFromCsv(String csvContent, String tableName, String username)
            throws IOException {
        String progressKey = username + "_" + tableName;

        List<String> data;
        try {
            try (Reader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8))) {
                data = parseCsv(reader);
            }
        } catch (HrmsApiException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("CSV parsing failed after chunked upload", ex);
            throw new HrmsApiException(
                    "Could not read the CSV file. Save as .csv (UTF-8), check commas and quotes, then try again.");
        }

        if (data.size() < 2) {
            throw new HrmsApiException(BulkUploadMessages.friendlyEmptyFileError());
        }

        int totalRows = data.size() - 1;
        UploadProgress progress = new UploadProgress(totalRows);
        uploadProgressMap.put(progressKey, progress);
        executorService.submit(() -> processUploadAsync(data, tableName, username, progress, totalRows));

        Map<String, String> resultData = new HashMap<>();
        resultData.put("progressKey", progressKey);
        resultData.put("totalRows", String.valueOf(totalRows));
        return ApiResponses.ok("Upload started. Please check progress.", Collections.singletonList(resultData));
    }

    private void validateChunkedUploadParams(
            String uploadId,
            int chunkIndex,
            int totalChunks,
            String tableName,
            String fileName) {
        if (uploadId == null || uploadId.isBlank() || uploadId.length() > 64) {
            throw new HrmsApiException("Invalid upload session.");
        }
        if (totalChunks < 1 || totalChunks > MAX_TOTAL_CHUNKS) {
            throw new HrmsApiException("Upload exceeds maximum allowed size.");
        }
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new HrmsApiException("Invalid chunk index.");
        }
        if (tableName == null || tableName.isBlank()) {
            throw new HrmsApiException("Table name is required.");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new HrmsApiException(BulkUploadMessages.friendlyFileNameError());
        }
    }

    private String decodeChunkBody(String chunkData, String encoding) {
        if (chunkData == null || chunkData.isEmpty()) {
            throw new HrmsApiException("Upload chunk is empty.");
        }
        if ("base64".equalsIgnoreCase(encoding)) {
            if (chunkData.getBytes(StandardCharsets.US_ASCII).length > MAX_ENCODED_CHUNK_BYTES) {
                throw new HrmsApiException("Upload chunk exceeds maximum allowed size.");
            }
            try {
                byte[] decoded = Base64.getDecoder().decode(chunkData.trim());
                if (decoded.length > MAX_CHUNK_BYTES) {
                    throw new HrmsApiException("Upload chunk exceeds maximum allowed size.");
                }
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                throw new HrmsApiException("Invalid upload chunk encoding.");
            }
        }
        if (chunkData.getBytes(StandardCharsets.UTF_8).length > MAX_CHUNK_BYTES) {
            throw new HrmsApiException("Upload chunk exceeds maximum allowed size.");
        }
        return chunkData;
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
            progress.message = ex.getClientMessage();
            progress.issues.clear();
            progress.issues.addAll(ex.getIssues());
        } catch (Exception ex) {
            logger.error("Error during async upload for {}", tableName, ex);
            progress.status = "ERROR";
            progress.message = UserFacingMessages.UPLOAD_FAILED;
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
        try (InputStream inputStream = formData.getInputStream();
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return parseCsv(reader);
        } catch (HrmsApiException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("CSV parsing failed", ex);
            throw new HrmsApiException(
                    "Could not read the CSV file. Save as .csv (UTF-8), check commas and quotes, then try again.");
        }
    }

    private List<String> parseCsv(Reader reader) throws IOException {
        List<String> data = new ArrayList<>();
        String[] headerNames = null;
        try (CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
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
                    String columnName = headerNames != null && i < headerNames.length
                            ? headerNames[i]
                            : "Column " + (i + 1);
                    if (cell != null
                            && BulkUploadValidation.shouldValidateCell(columnName)
                            && !StringUtils.isAlphanumericSpace(CommonUtils.customReplace(cell, excelValidation))) {
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
        }
        return data;
    }
}
