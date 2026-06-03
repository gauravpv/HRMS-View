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

    @JsonProperty("lastActivitySummary")
    private String lastActivitySummary;

    @JsonProperty("lastActivityAt")
    private String lastActivityAt;

    @JsonProperty("cached")
    private boolean cached;
}
