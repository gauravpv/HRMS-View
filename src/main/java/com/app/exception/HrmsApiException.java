package com.app.exception;

import java.util.Collections;
import java.util.List;

import com.app.dto.UploadIssue;
import com.app.support.UserFacingMessages;

/**
 * API-level failure returned to the client as {@code AjaxError}.
 * Use {@link #internal(String, String)} for system/DB errors — full detail is logged server-side only.
 */
public class HrmsApiException extends RuntimeException {

    private final Integer row;
    private final Integer column;
    private final String columnName;
    private final List<UploadIssue> issues;
    private final String clientMessage;

    public HrmsApiException(String message) {
        this(message, message, null, null, null, null);
    }

    public HrmsApiException(String message, int row, int column, String columnName, String value) {
        this(message, message, row, column, columnName, value == null ? null : List.of(new UploadIssue(
                row,
                column,
                columnName,
                value,
                "Invalid characters or format — use the template for allowed values.")));
    }

    public HrmsApiException(String message, List<UploadIssue> issues) {
        this(message, message, null, null, null, issues);
    }

    /**
     * Technical failure: {@code logMessage} is logged; {@code clientMessage} is shown in the UI.
     */
    public static HrmsApiException internal(String logMessage, String clientMessage) {
        return new HrmsApiException(logMessage, clientMessage, null, null, null, null);
    }

    public static HrmsApiException internal(String logMessage) {
        return internal(logMessage, UserFacingMessages.OPERATION_FAILED);
    }

    private HrmsApiException(
            String logMessage,
            String clientMessage,
            Integer row,
            Integer column,
            String columnName,
            List<UploadIssue> issues) {
        super(logMessage);
        this.clientMessage = clientMessage != null ? clientMessage : UserFacingMessages.OPERATION_FAILED;
        this.row = row;
        this.column = column;
        this.columnName = columnName;
        this.issues = issues == null ? Collections.emptyList() : List.copyOf(issues);
    }

    /** Message safe to return to the browser. */
    public String getClientMessage() {
        return clientMessage;
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

    public boolean isUserFacingValidation() {
        return row != null || !issues.isEmpty();
    }
}
