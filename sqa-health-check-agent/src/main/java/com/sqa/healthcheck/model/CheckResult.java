package com.sqa.healthcheck.model;

import java.time.LocalDateTime;

/**
 * Outcome of checking a single system: status, how long it took,
 * and (if it failed) a specific reason so the portal can distinguish
 * "site didn't load" from "login rejected" from "dashboard didn't appear".
 */
public class CheckResult {
    public String name;
    public String url;
    public Status status;
    public long responseTimeMillis;
    public String failureReason; // null if status == UP
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
