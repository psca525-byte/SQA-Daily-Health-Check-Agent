package com.sqa.healthcheck.core;

import com.sqa.healthcheck.model.CheckResult;
import com.sqa.healthcheck.model.SystemConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Base64;

/**
 * Performs one system's health check: open URL -> log in -> verify a
 * post-login element appears. Records timing and a specific failure
 * reason so the dashboard can tell testers exactly what stage failed.
 */
public class SystemChecker {

    private static final Logger logger = LogManager.getLogger(SystemChecker.class);
    private static final int WAIT_SECONDS = 45;

    public CheckResult check(WebDriver driver, SystemConfig config) {
        if ("PING".equalsIgnoreCase(config.checkType)) {
            return checkPageLoadOnly(driver, config);
        }
        return checkFullLogin(driver, config);
    }

    private CheckResult checkPageLoadOnly(WebDriver driver, SystemConfig config) {
        CheckResult result = new CheckResult(config.name, config.url);
        long startTime = System.currentTimeMillis();

        try {
            driver.get(config.url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));

            if (config.pageLoadedLocator == null || config.pageLoadedLocator.isBlank()) {
                throw new CheckFailedException(
                        "checkType is PING but 'pageLoadedLocator' is not set in config for this system");
            }

            safeFind(wait, config.pageLoadedLocator,
                    "Page loaded but expected element was not found (page may be broken, or locator is outdated)");

            result.status = CheckResult.Status.UP;

        } catch (CheckFailedException e) {
            result.status = CheckResult.Status.DOWN;
            result.failureReason = e.getMessage();
            logger.warn("[{}] DOWN - {}", config.name, e.getMessage());
        } catch (TimeoutException e) {
            result.status = CheckResult.Status.DOWN;
            result.failureReason = "Page took too long to load (timeout after " + WAIT_SECONDS + "s)";
            logger.warn("[{}] DOWN - timeout", config.name);
        } catch (Exception e) {
            result.status = CheckResult.Status.DOWN;
            result.failureReason = "Unexpected error: " + e.getClass().getSimpleName()
                    + (e.getMessage() != null ? " - " + e.getMessage() : "");
            logger.error("[{}] DOWN - unexpected error", config.name, e);
        } finally {
            result.screenshotBase64 = captureScreenshot(driver);
            result.responseTimeMillis = System.currentTimeMillis() - startTime;
        }

        return result;
    }

    private CheckResult checkFullLogin(WebDriver driver, SystemConfig config) {
        CheckResult result = new CheckResult(config.name, config.url);
        long startTime = System.currentTimeMillis();

        try {
            String username = resolveCredential(config.usernameEnvVar, "username");
            String password = resolveCredential(config.passwordEnvVar, "password");

            driver.get(config.url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));

            WebElement usernameField = safeFind(wait, config.usernameLocator,
                    "Username field not found (page may not have loaded, or locator is outdated)");
            usernameField.clear();
            usernameField.sendKeys(username);

            WebElement passwordField = safeFind(wait, config.passwordLocator,
                    "Password field not found (locator may be outdated)");
            passwordField.clear();
            passwordField.sendKeys(password);

            WebElement submitButton = safeFind(wait, config.submitLocator,
                    "Login button not found (locator may be outdated)");
            submitButton.click();

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
            result.screenshotBase64 = captureScreenshot(driver);
            result.responseTimeMillis = System.currentTimeMillis() - startTime;
        }

        return result;
    }

    private String captureScreenshot(WebDriver driver) {
        try {
            byte[] pngBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            return Base64.getEncoder().encodeToString(pngBytes);
        } catch (Exception e) {
            logger.warn("Could not capture screenshot: {}", e.getMessage());
            return null;
        }
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

    private static class CheckFailedException extends RuntimeException {
        CheckFailedException(String message) {
            super(message);
        }
    }
}
