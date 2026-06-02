package com.app.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public class BulkInsertResult {

    private static final int MAX_ISSUES = 50;

    private int errorCount;
    private final List<UploadIssue> issues = new ArrayList<>();

    public void addIssue(int row, Integer column, String columnName, String value, String message) {
        errorCount++;
        if (issues.size() < MAX_ISSUES) {
            issues.add(new UploadIssue(row, column, columnName, truncate(value, 80), message));
        }
    }

    public void addIssue(int row, String message) {
        addIssue(row, null, null, null, message);
    }

    public void merge(BulkInsertResult other) {
        if (other == null) {
            return;
        }
        errorCount += other.errorCount;
        for (UploadIssue issue : other.issues) {
            if (issues.size() >= MAX_ISSUES) {
                break;
            }
            issues.add(issue);
        }
    }

    public List<UploadIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max - 3) + "...";
    }
}
