package com.app.dto;

import java.sql.Timestamp;
import java.util.List;

import lombok.Data;

@Data
public class AjaxError {

    private String errorMsg;
    private Timestamp time;
    private Integer row;
    private Integer column;
    private String columnName;
    private List<UploadIssue> issues;
}
