package com.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogRow {

    @JsonProperty("id")
    private int id;

    @JsonProperty("loggedAt")
    private String loggedAt;

    @JsonProperty("user")
    private String user;

    @JsonProperty("action")
    private String action;

    @JsonProperty("actionLabel")
    private String actionLabel;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("template")
    private String template;

    @JsonProperty("displayTemplate")
    private String displayTemplate;

    @JsonProperty("summary")
    private String summary;
}
