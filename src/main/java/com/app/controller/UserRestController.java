package com.app.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.app.dto.AjaxBody;
import com.app.dto.DashboardSummary;
import com.app.dto.StringListDto;
import com.app.dto.TableStatusRow;
import com.app.exception.HrmsApiException;
import com.app.service.ActivityLogService;
import com.app.service.GeneralService;
import com.app.service.TableDetailsService;
import com.app.service.TableStatusService;
import com.app.utility.CommonUtils;
import com.app.utility.StringUtils;
import com.app.dto.HistorySnapshotDto;
import com.app.support.HistorySnapshotMapper;
import com.app.web.ApiResponses;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserRestController {

    private static final Logger logger = LogManager.getLogger(UserRestController.class);

    private final GeneralService genService;
    private final TableDetailsService tabService;
    private final TableStatusService tableStatusService;
    private final ActivityLogService activityLogService;

    @Value("${excel.validation}")
    private String excelValidation;

    @GetMapping("/tableStatus")
    public ResponseEntity<?> tableStatus(
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        try {
            List<TableStatusRow> rows = tableStatusService.getMainTableStatus(refresh);
            return ApiResponses.ok("Table status retrieved", rows);
        } catch (Exception ex) {
            logger.error("Table status request failed", ex);
            return ApiResponses.clientError(
                    "Unable to load table status. Check database connectivity and table_details access.");
        }
    }

    @GetMapping("/activityLogs")
    public ResponseEntity<?> activityLogs(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        try {
            return ApiResponses.okValue("Activity logs retrieved", activityLogService.getRecentLogs(limit));
        } catch (Exception ex) {
            logger.error("Activity logs request failed", ex);
            return ApiResponses.clientError("Unable to load activity logs.");
        }
    }

    @GetMapping("/dashboardSummary")
    public ResponseEntity<?> dashboardSummary(
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        try {
            DashboardSummary summary = tableStatusService.getDashboardSummary(refresh);
            return ApiResponses.okValue("Dashboard summary retrieved", summary);
        } catch (Exception ex) {
            logger.error("Dashboard summary request failed", ex);
            return ApiResponses.clientError("Unable to load dashboard summary.");
        }
    }

    @GetMapping("/searchTableData")
    public ResponseEntity<AjaxBody> searchTableData(@RequestParam String tabName) {
        logger.debug("searchTableData tabName={}", tabName);
        List<Object> list = genService.getAllData(tabName);
        return ApiResponses.ok("List retrieved", list);
    }

    @GetMapping("/historyTableData")
    public ResponseEntity<AjaxBody> historyTableData(@RequestParam String tabName, @RequestParam int historyId) {
        logger.info("historyTableData tabName={} historyId={}", tabName, historyId);
        try {
            List<Object> list = genService.getHistoryData(tabName, historyId);
            logger.info("historyTableData tabName={} historyId={} rows={}", tabName, historyId, list.size());
            return ApiResponses.ok("List retrieved", list);
        } catch (HrmsApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            logger.error("historyTableData failed tabName={} historyId={}", tabName, historyId, ex);
            throw HrmsApiException.internal(
                    "historyTableData failed tabName=" + tabName + " historyId=" + historyId + ": " + ex.getMessage());
        }
    }

    @GetMapping("/historyTableId")
    public ResponseEntity<AjaxBody> historyTableId(
            @RequestParam String tabName,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        logger.info("historyTableId tabName={} from={} to={}", tabName, fromDate, toDate);
        try {
            List<Object> list = genService.getHistoryId(tabName, fromDate, toDate);
            List<HistorySnapshotDto> snapshots = HistorySnapshotMapper.normalize(list);
            if (!list.isEmpty() && snapshots.isEmpty()) {
                Object sample = list.get(0);
                logger.warn("historyTableId tabName={} rawRows={} normalized=0 sampleType={} sample={}",
                        tabName, list.size(), sample != null ? sample.getClass().getSimpleName() : "null", sample);
            } else if (!snapshots.isEmpty()) {
                HistorySnapshotDto first = snapshots.get(0);
                logger.info("historyTableId tabName={} snapshots={} firstId={} firstDate={}",
                        tabName, snapshots.size(), first.getHistoryId(), first.getSnapshotDate());
            } else {
                logger.info("historyTableId tabName={} snapshots=0", tabName);
            }
            return ApiResponses.ok("List retrieved", snapshots);
        } catch (HrmsApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            logger.error("historyTableId failed tabName={} from={} to={}", tabName, fromDate, toDate, ex);
            throw HrmsApiException.internal("historyTableId failed tabName=" + tabName + ": " + ex.getMessage());
        }
    }

    @PostMapping("/saveTempData")
    public ResponseEntity<AjaxBody> saveTempData(StringListDto data, Principal principal) {
        logger.debug("saveTempData user={}", principal.getName());
        String msg = genService.saveTempData(data.getStringList(), principal.getName());
        return ApiResponses.ok(msg);
    }

    @GetMapping("/getAllCreatedData")
    public ResponseEntity<AjaxBody> getAllCreatedData() {
        logger.debug("getAllCreatedData");
        Map<String, List<LinkedCaseInsensitiveMap<?>>> retrievedList = genService.getAllDataByAction("CREATED");
        List<Map<String, List<LinkedCaseInsensitiveMap<?>>>> list = new ArrayList<>();
        list.add(retrievedList);
        return ApiResponses.ok("Created data retreived", list);
    }

    @PostMapping("/addFile")
    public ResponseEntity<AjaxBody> addTableDetail(
            @RequestParam("file") MultipartFile formData,
            @RequestParam("tableName") String tableName,
            Principal principal) throws IOException {
        logger.debug("addFile table={} file={}", tableName, formData.getOriginalFilename());
        validateFileName(formData.getOriginalFilename());

        String existingDataMessage = genService.checkDataExists(tableName);
        if (!"No data".equals(existingDataMessage)) {
            throw new HrmsApiException(existingDataMessage);
        }
        if (formData.isEmpty()) {
            throw new HrmsApiException("Excel empty");
        }

        List<String> data = parseCsv(formData);
        if (data.isEmpty()) {
            throw new HrmsApiException("Excel empty");
        }

        String username = principal.getName();
        String headers = data.get(0).substring(0, data.get(0).length() - 1).replace("|", ",");
        String msg = "";
        for (int i = 1; i < data.size(); i++) {
            String value = data.get(i).substring(0, data.get(i).length() - 1).replace("|", ",");
            logger.debug("Bulk row table={} headers={}", tableName, headers);
            msg = genService.addBulkTempData(headers, value, tableName, username);
        }

        if ("Error".equals(msg)) {
            throw new HrmsApiException("Error in data upload. Please contact admin");
        }
        int rowCount = Math.max(0, data.size() - 1);
        activityLogService.recordUpload(tableName, username, rowCount + " row(s)");
        return ApiResponses.ok("Added data", Collections.emptyList());
    }

    @GetMapping("/getColumns")
    public ResponseEntity<AjaxBody> getAllColumns(@RequestParam String tabName) {
        logger.debug("getColumns tabName={}", tabName);
        String columnNames = tabService.getColumnNames(tabName);
        return ApiResponses.ok("columns retreived", List.of(columnNames));
    }

    @GetMapping("/truncateTable")
    public ResponseEntity<AjaxBody> truncateTable(@RequestParam String tableName) {
        logger.info("truncateTable table={}", tableName);
        String msg = genService.truncateTable(tableName);
        return ApiResponses.ok(msg);
    }

    @GetMapping("/moveToHistory")
    public ResponseEntity<AjaxBody> moveToHistory(@RequestParam String tableName) {
        logger.info("moveToHistory table={}", tableName);
        String msg = genService.moveToHistory(tableName);
        return ApiResponses.ok(msg);
    }

    @PostMapping("/masterDataMovement")
    public ResponseEntity<AjaxBody> masterDataMovement(StringListDto tableNames, Principal principal) {
        List<String> tables = resolveMovementTables(tableNames);
        logger.info("masterDataMovement tables={} user={}", tables, principal.getName());
        String msg = genService.masterDataMove(tables);
        recordMovementActivitySafe(tables, principal.getName(), true);
        return ApiResponses.ok(msg);
    }

    @PostMapping("/mainDataMovement")
    public ResponseEntity<AjaxBody> mainDataMovement(StringListDto tableNames, Principal principal) {
        List<String> tables = resolveMovementTables(tableNames);
        logger.info("mainDataMovement tables={} user={}", tables, principal.getName());
        String msg = genService.mainDataMove(tables);
        recordMovementActivitySafe(tables, principal.getName(), false);
        return ApiResponses.ok(msg);
    }

    private static List<String> resolveMovementTables(StringListDto tableNames) {
        if (tableNames == null || tableNames.getStringList() == null || tableNames.getStringList().isEmpty()) {
            throw new HrmsApiException(
                    "No tables were received by the server. Select at least one table and try again.");
        }
        return tableNames.getStringList();
    }

    private void recordMovementActivitySafe(List<String> tables, String username, boolean toMaster) {
        try {
            activityLogService.recordMovement(tables, username, toMaster);
        } catch (Exception ex) {
            logger.warn("Activity log failed after successful data movement user={} tables={}", username, tables, ex);
        }
    }

    private void validateFileName(String fileName) {
        if (!StringUtils.isAlphanumericSpace(CommonUtils.customReplace(fileName, "_.()"))) {
            throw new HrmsApiException("Error in file name format");
        }
    }

    private List<String> parseCsv(MultipartFile formData) throws IOException {
        List<String> data = new ArrayList<>();
        try (InputStream inputStream = formData.getInputStream();
                Reader reader = new BufferedReader(new InputStreamReader(inputStream));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            for (CSVRecord csvRecord : csvParser) {
                StringBuilder values = new StringBuilder();
                for (int i = 0; i < csvRecord.size(); i++) {
                    String cell = csvRecord.get(i);
                    if (cell != null
                            && !StringUtils.isAlphanumericSpace(CommonUtils.customReplace(cell, excelValidation))) {
                        throw new HrmsApiException(
                                "Data validation error for the field: " + cell
                                        + " in Row: " + csvRecord.getRecordNumber()
                                        + " and Column: " + (i + 1));
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
            throw new HrmsApiException("Error parsing CSV file");
        }
        return data;
    }
}
