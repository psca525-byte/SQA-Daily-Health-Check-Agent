package com.sqa.healthcheck.model;

/**
 * Represents one system's configuration as read from config/systems.json.
 * Locators are given as "strategy:value", e.g. "id:username" or "css:.login-btn"
 * so non-developers can edit the JSON file without touching Java code.
 */
public class SystemConfig {
    public String name;
    public String url;

    /**
     * "LOGIN" (default if omitted) - full login-and-verify check.
     * "PING"  - just verifies the page itself loads and a known element
     *           appears, WITHOUT attempting to log in. Use this for any
     *           system protected by CAPTCHA, since automating past a
     *           CAPTCHA is not something this tool will do.
     */
    public String checkType = "LOGIN";

    public String usernameLocator;
    public String passwordLocator;
    public String submitLocator;
    public String successLocator;
    public String usernameEnvVar;
    public String passwordEnvVar;

    // Used only when checkType = "PING": an element that should appear on
    // the page if it loaded correctly, e.g. the login form itself or logo.
    public String pageLoadedLocator;

    // Optional (PING mode only): if the site shows a popup/modal on load,
    // this is the locator for its close button. It will be clicked once,
    // if found, before checking pageLoadedLocator. Safe to leave unset if
    // there's no popup - the check simply skips this step.
    public String popupCloseLocator;
}
