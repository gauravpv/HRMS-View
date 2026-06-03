package com.app.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.dto.ActivityLogRow;
import com.app.dto.DashboardSummary;
import com.app.model.RequestResponseLogDetails;
import com.app.repository.RequestResponseLogDetailsRepository;
import com.app.service.ActivityLogService;
import com.app.support.ActivityLogActions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final Logger logger = LogManager.getLogger(ActivityLogServiceImpl.class);

    private static final Pattern ISO_PREFIX = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;

    private static final String SYSTEM_TABLE = "—";

    private static final Set<String> DASHBOARD_ACTIONS = Set.of(
            ActivityLogActions.UPLOAD,
            ActivityLogActions.MOVE_TO_MASTER,
            ActivityLogActions.MOVE_TO_MAIN,
            ActivityLogActions.LOGIN,
            ActivityLogActions.LOGOUT);

    private final RequestResponseLogDetailsRepository logRepo;

    @Override
    public void recordUpload(String tableName, String updatedBy, String details) {
        record(tableName, updatedBy, ActivityLogActions.UPLOAD, details);
    }

    @Override
    public void recordMovement(List<String> masterTableNames, String updatedBy, boolean toMaster) {
        if (masterTableNames == null || masterTableNames.isEmpty()) {
            return;
        }
        String action = toMaster ? ActivityLogActions.MOVE_TO_MASTER : ActivityLogActions.MOVE_TO_MAIN;
        for (String tableName : masterTableNames) {
            record(tableName, updatedBy, action, "");
        }
    }

    @Override
    public void recordLogin(String username) {
        recordSafe(SYSTEM_TABLE, username, ActivityLogActions.LOGIN, "");
    }

    @Override
    public void recordLogout(String username) {
        recordSafe(SYSTEM_TABLE, username, ActivityLogActions.LOGOUT, "");
    }

    private void record(String tableName, String updatedBy, String action, String details) {
        recordSafe(tableName, updatedBy, action, details);
    }

    private void recordSafe(String tableName, String updatedBy, String action, String details) {
        try {
            String state = Instant.now().toString() + "|" + (details != null ? details : "");
            logRepo.save(new RequestResponseLogDetails("", state, tableName, updatedBy, action));
        } catch (Exception ex) {
            logger.warn("Failed to write activity log action={} user={}", action, updatedBy, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary buildActivityOverview() {
        DashboardSummary summary = new DashboardSummary();
        List<ActivityLogRow> logs = getRecentLogs(1);
        if (!logs.isEmpty()) {
            ActivityLogRow latest = logs.get(0);
            summary.setLastActivitySummary(latest.getSummary());
            summary.setLastActivityAt(latest.getLoggedAt());
        }
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogRow> getRecentLogs(int limit) {
        int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        List<RequestResponseLogDetails> rows = logRepo.findTop100ByActionInOrderByIdDesc(DASHBOARD_ACTIONS);
        List<ActivityLogRow> result = new ArrayList<>(Math.min(capped, rows.size()));
        for (RequestResponseLogDetails row : rows) {
            if (result.size() >= capped) {
                break;
            }
            result.add(toDto(row));
        }
        return result;
    }

    private static ActivityLogRow toDto(RequestResponseLogDetails row) {
        String template = row.getTableName() != null ? row.getTableName() : "";
        String action = row.getAction() != null ? row.getAction() : "";
        String user = row.getUpdatedBy() != null ? row.getUpdatedBy() : "";
        String displayTemplate = displayTemplateName(template);
        ParsedState parsed = parseNewState(row.getNewState());
        String summary = buildSummary(user, action, displayTemplate, parsed.details);
        return new ActivityLogRow(
                row.getId() != null ? row.getId() : 0,
                parsed.loggedAt,
                user,
                action,
                ActivityLogActions.labelFor(action),
                ActivityLogActions.iconFor(action),
                template,
                displayTemplate,
                summary);
    }

    private static String buildSummary(String user, String action, String displayTemplate, String details) {
        String who = user.isBlank() ? "Someone" : user;
        String templateLabel = displayTemplate.isBlank() ? "a template" : displayTemplate + " template";
        if (ActivityLogActions.UPLOAD.equals(action)) {
            if (details != null && !details.isBlank()) {
                return who + " uploaded " + templateLabel + " (" + details + ")";
            }
            return who + " uploaded " + templateLabel;
        }
        if (ActivityLogActions.MOVE_TO_MASTER.equals(action)) {
            return who + " moved " + templateLabel + " to master";
        }
        if (ActivityLogActions.MOVE_TO_MAIN.equals(action)) {
            return who + " moved " + templateLabel + " to main";
        }
        if (ActivityLogActions.LOGIN.equals(action)) {
            return who + " signed in";
        }
        if (ActivityLogActions.LOGOUT.equals(action)) {
            return who + " signed out";
        }
        return who + " performed an action on " + templateLabel;
    }

    private static ParsedState parseNewState(String newState) {
        if (newState == null || newState.isBlank()) {
            return new ParsedState(null, "");
        }
        int pipe = newState.indexOf('|');
        if (pipe > 0) {
            String prefix = newState.substring(0, pipe);
            if (ISO_PREFIX.matcher(prefix).find()) {
                return new ParsedState(prefix, newState.substring(pipe + 1));
            }
        }
        return new ParsedState(null, newState);
    }

    private static String displayTemplateName(String tableName) {
        if (tableName == null || tableName.isBlank() || SYSTEM_TABLE.equals(tableName)) {
            return "";
        }
        return tableName.replaceAll("(?i)_master$", "").replaceAll("(?i)_temp$", "").toUpperCase();
    }

    private record ParsedState(String loggedAt, String details) {
    }
}
