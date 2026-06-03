package com.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    @JsonProperty("tableCount")
    private int tableCount;

    @JsonProperty("totalRecords")
    private long totalRecords;

    @JsonProperty("latestUpdated")
    private String latestUpdated;

    @JsonProperty("latestTableName")
    private String latestTableName;

    @JsonProperty("unavailableCount")
    private int unavailableCount;

    @JsonProperty("cached")
    private boolean cached;
}
