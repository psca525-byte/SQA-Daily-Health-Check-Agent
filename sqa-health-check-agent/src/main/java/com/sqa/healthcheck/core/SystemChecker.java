package com.sqa.healthcheck.core;

import com.sqa.healthcheck.model.CheckResult;
import com.sqa.healthcheck.model.SystemConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Performs one system's health check: open URL -> log in -> verify a
 * post-login element appears. Records timing and a specific failure
 * reason so the dashboard can tell testers exactly what stage failed.
 */
public class SystemChecker {

    private static final Logger logger = LogManager.getLogger(SystemChecker.class);
    private static final int WAIT_SECONDS = 20;

    public CheckResult check(WebDriver driver, SystemConfig config) {
        CheckResult result = new CheckResult(config.name, config.url);
        long startTime = System.currentTimeMillis();

        try {
            String username = resolveCredential(config.usernameEnvVar, "username");
            String password = resolveCredential(config.passwordEnvVar, "password");

            driver.get(config.url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));

            // Step 1: locate and fill username
            WebElement usernameField = safeFind(wait, config.usernameLocator,
                    "Username field not found (page may not have loaded, or locator is outdated)");
            usernameField.clear();
            usernameField.sendKeys(username);

            // Step 2: locate and fill password
            WebElement passwordField = safeFind(wait, config.passwordLocator,
                    "Password field not found (locator may be outdated)");
            passwordField.clear();
            passwordField.sendKeys(password);

            // Step 3: click submit
            WebElement submitButton = safeFind(wait, config.submitLocator,
                    "Login button not found (locator may be outdated)");
            submitButton.click();

            // Step 4: verify a post-login element appears -> proves login actually succeeded
            try {
                wait.until(ExpectedConditions.visibilityOf(
                        driver.findElement(LocatorParser.parse(config.successLocator))));
            } catch (Exception e) {
                throw new CheckFailedException(
                        "Login submitted but expected post-login element did not appear "
                                + "(credentials may be wrong, or account locked, or page structure changed)");
            }

            result.status = CheckResult.Status.UP;

        } catch (CheckFailedException e) {
            result.status = CheckResult.Status.DOWN;
            result.failureReason = e.getMessage();
            logger.warn("[{}] DOWN - {}", config.name, e.getMessage());
        } catch (TimeoutException e) {
            result.status = CheckResult.Status.DOWN;
            result.failureReason = "Page took too long to load or element never appeared (timeout after "
                    + WAIT_SECONDS + "s)";
            logger.warn("[{}] DOWN - timeout", config.name);
        } catch (Exception e) {
            result.status = CheckResult.Status.DOWN;
            result.failureReason = "Unexpected error: " + e.getClass().getSimpleName()
                    + (e.getMessage() != null ? " - " + e.getMessage() : "");
            logger.error("[{}] DOWN - unexpected error", config.name, e);
        } finally {
            result.responseTimeMillis = System.currentTimeMillis() - startTime;
        }

        return result;
    }

    private WebElement safeFind(WebDriverWait wait, String locatorString, String failureMessage) {
        try {
            By by = LocatorParser.parse(locatorString);
            return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        } catch (TimeoutException e) {
            throw new CheckFailedException(failureMessage);
        }
    }

    private String resolveCredential(String envVarName, String fieldLabel) {
        String value = System.getenv(envVarName);
        if (value == null || value.isBlank()) {
            throw new CheckFailedException(
                    "Missing " + fieldLabel + " - environment variable '" + envVarName + "' is not set");
        }
        return value;
    }

    /**
     * Internal exception used to short-circuit with a specific, human-readable
     * failure reason rather than a raw stack trace.
     */
    private static class CheckFailedException extends RuntimeException {
        CheckFailedException(String message) {
            super(message);
        }
    }
}
