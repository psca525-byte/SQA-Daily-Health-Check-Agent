package com.sqa.healthcheck.model;

/**
 * Represents one system's configuration as read from config/systems.json.
 * Locators are given as "strategy:value", e.g. "id:username" or "css:.login-btn"
 * so non-developers can edit the JSON file without touching Java code.
 */
public class SystemConfig {
    public String name;
    public String url;
    public String usernameLocator;
    public String passwordLocator;
    public String submitLocator;
    public String successLocator;
    public String usernameEnvVar;
    public String passwordEnvVar;
}
