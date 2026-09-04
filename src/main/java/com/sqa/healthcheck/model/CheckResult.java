package com.sqa.healthcheck.model;

import java.time.LocalDateTime;

public class CheckResult {
    public String name;
    public String url;
    public Status status;
    public long responseTimeMillis;
    public String failureReason; // null if status == UP
    public String screenshotBase64; // captured on every check, UP or DOWN
    public LocalDateTime checkedAt;

    public enum Status {
        UP,
        DOWN
    }

    public CheckResult(String name, String url) {
        this.name = name;
        this.url = url;
        this.checkedAt = LocalDateTime.now();
    }
}
