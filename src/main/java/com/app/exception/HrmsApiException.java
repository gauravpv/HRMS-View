package com.app.exception;

import java.util.Collections;
import java.util.List;

import com.app.dto.UploadIssue;

/**
 * API-level failure returned to the client as {@code AjaxError} (validation or business rule).
 */
public class HrmsApiException extends RuntimeException {

    private final Integer row;
    private final Integer column;
    private final String columnName;
    private final List<UploadIssue> issues;

    public HrmsApiException(String message) {
        this(message, null, null, null, null);
    }

    public HrmsApiException(String message, int row, int column, String columnName, String value) {
        this(message, row, column, columnName, value == null ? null : List.of(new UploadIssue(
                row,
                column,
                columnName,
                value,
                "Invalid characters or format — use the template for allowed values.")));
    }

    public HrmsApiException(String message, List<UploadIssue> issues) {
        this(message, null, null, null, issues);
    }

    private HrmsApiException(String message, Integer row, Integer column, String columnName, List<UploadIssue> issues) {
        super(message);
        this.row = row;
        this.column = column;
        this.columnName = columnName;
        this.issues = issues == null ? Collections.emptyList() : List.copyOf(issues);
    }

    public Integer getRow() {
        return row;
    }

    public Integer getColumn() {
        return column;
    }

    public String getColumnName() {
        return columnName;
    }

    public List<UploadIssue> getIssues() {
        return issues;
    }
}
