package com.app.web;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.app.dto.AjaxBody;
import com.app.dto.AjaxError;
import com.app.dto.UploadIssue;
import com.app.exception.HrmsApiException;

public final class ApiResponses {

    private ApiResponses() {
    }

    public static ResponseEntity<AjaxBody> ok(String message) {
        return ok(message, null);
    }

    public static ResponseEntity<AjaxBody> ok(String message, List<?> result) {
        AjaxBody body = new AjaxBody();
        body.setMsg(message);
        body.setResult(result);
        return ResponseEntity.ok(body);
    }

    public static ResponseEntity<AjaxError> error(HttpStatus status, String message) {
        AjaxError err = new AjaxError();
        err.setErrorMsg(message);
        err.setTime(new Timestamp(System.currentTimeMillis()));
        return ResponseEntity.status(status).body(err);
    }

    /** Matches legacy AJAX endpoints that use HTTP 500 for validation/business errors. */
    public static ResponseEntity<AjaxError> clientError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static ResponseEntity<AjaxError> clientError(HrmsApiException ex) {
        AjaxError err = new AjaxError();
        err.setErrorMsg(ex.getMessage());
        err.setTime(new Timestamp(System.currentTimeMillis()));
        err.setRow(ex.getRow());
        err.setColumn(ex.getColumn());
        err.setColumnName(ex.getColumnName());
        if (!ex.getIssues().isEmpty()) {
            err.setIssues(ex.getIssues());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    public static ResponseEntity<AjaxError> clientError(String message, List<UploadIssue> issues) {
        AjaxError err = new AjaxError();
        err.setErrorMsg(message);
        err.setTime(new Timestamp(System.currentTimeMillis()));
        err.setIssues(issues);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
