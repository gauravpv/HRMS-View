package com.app.service;

import java.util.List;

import com.app.dto.ActivityLogRow;
import com.app.dto.DashboardSummary;

public interface ActivityLogService {

    void recordUpload(String tableName, String updatedBy, String details);

    void recordMovement(List<String> masterTableNames, String updatedBy, boolean toMaster);

    void recordLogin(String username);

    void recordLogout(String username);

    List<ActivityLogRow> getRecentLogs(int limit);

    DashboardSummary buildActivityOverview();
}
