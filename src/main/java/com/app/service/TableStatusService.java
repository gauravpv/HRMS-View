package com.app.service;

import java.util.List;

import com.app.dto.DashboardSummary;
import com.app.dto.TableStatusRow;

public interface TableStatusService {

    List<TableStatusRow> getMainTableStatus();

    List<TableStatusRow> getMainTableStatus(boolean forceRefresh);

    DashboardSummary getDashboardSummary(boolean forceRefresh);
}
