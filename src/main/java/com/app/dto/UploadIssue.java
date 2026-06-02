package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadIssue {

    private Integer row;
    private Integer column;
    private String columnName;
    private String value;
    private String message;
}
