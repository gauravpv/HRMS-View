package com.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableStatusRow {

    @JsonProperty("tableName")
    private String tableName;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("recordCount")
    private long recordCount;

    @JsonProperty("lastUpdated")
    private String lastUpdated;
}
