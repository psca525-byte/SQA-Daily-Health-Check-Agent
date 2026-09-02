package com.sqa.healthcheck.core;

import org.openqa.selenium.By;

/**
 * Converts a locator string from config (e.g. "id:username", "css:.login-btn",
 * "xpath://div[@id='x']", "name:email") into a Selenium By object.
 * This is what lets non-developers add new systems just by editing JSON.
 */
public class LocatorParser {

    public static By parse(String locator) {
        if (locator == null || !locator.contains(":")) {
            throw new IllegalArgumentException(
                    "Invalid locator format: '" + locator + "'. Expected 'strategy:value', e.g. 'id:username'");
        }

        int splitIndex = locator.indexOf(':');
        String strategy = locator.substring(0, splitIndex).trim().toLowerCase();
        String value = locator.substring(splitIndex + 1).trim();

        return switch (strategy) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "css" -> By.cssSelector(value);
            case "xpath" -> By.xpath(value);
            case "class" -> By.className(value);
            case "linktext" -> By.linkText(value);
            default -> throw new IllegalArgumentException(
                    "Unknown locator strategy: '" + strategy + "'. Use id, name, css, xpath, class, or linktext.");
        };
    }
}
