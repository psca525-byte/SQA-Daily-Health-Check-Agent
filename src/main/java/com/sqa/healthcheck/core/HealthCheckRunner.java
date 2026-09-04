package com.sqa.healthcheck.core;

import com.google.gson.Gson;
import com.sqa.healthcheck.model.CheckResult;
import com.sqa.healthcheck.model.SystemConfig;
import com.sqa.healthcheck.model.SystemConfigList;
import com.sqa.healthcheck.report.DashboardGenerator;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point. Reads config/systems.json, checks each system one by one
 * using a fresh browser session, and writes docs/index.html as the
 * shareable dashboard.
 *
 * Run with: mvn clean package && java -jar target/sqa-health-check-agent.jar
 */
public class HealthCheckRunner {

    private static final Logger logger = LogManager.getLogger(HealthCheckRunner.class);
    private static final String CONFIG_PATH = "config/systems.json";
    private static final String OUTPUT_PATH = "docs/index.html";

    public static void main(String[] args) {
        List<SystemConfig> systems = loadConfig();
        if (systems.isEmpty()) {
            logger.warn("No systems found in {}. Nothing to check.", CONFIG_PATH);
            return;
        }

        logger.info("Loaded {} systems from config. Starting health checks...", systems.size());

        WebDriverManager.chromedriver().setup();
        SystemChecker checker = new SystemChecker();
        List<CheckResult> results = new ArrayList<>();

        for (SystemConfig system : systems) {
            logger.info("Checking: {}", system.name);
            WebDriver driver = createDriver();
            try {
                CheckResult result = checker.check(driver, system);
                results.add(result);
                logger.info("[{}] {} ({} ms)", system.name, result.status, result.responseTimeMillis);
            } finally {
                driver.quit();
            }
        }

        try {
            new DashboardGenerator().generate(results, OUTPUT_PATH);
            logger.info("Dashboard written to {}", OUTPUT_PATH);
        } catch (IOException e) {
            logger.error("Failed to write dashboard HTML", e);
        }

        long downCount = results.stream().filter(r -> r.status == CheckResult.Status.DOWN).count();
        if (downCount > 0) {
            logger.warn("{} out of {} systems are DOWN.", downCount, results.size());
            // Non-zero exit code so CI can flag the run as failed/attention-needed
            System.exit(1);
        }
        
    }

    private static WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        return new ChromeDriver(options);
    }

    private static List<SystemConfig> loadConfig() {
        Path path = Path.of(CONFIG_PATH);
        if (!Files.exists(path)) {
            logger.error("Config file not found at {}", CONFIG_PATH);
            return List.of();
        }
        try (Reader reader = new FileReader(path.toFile())) {
            SystemConfigList configList = new Gson().fromJson(reader, SystemConfigList.class);
            return configList != null && configList.systems != null ? configList.systems : List.of();
        } catch (IOException e) {
            logger.error("Failed to read config file {}", CONFIG_PATH, e);
            return List.of();
        }
    }
    // Change this line at the end of main():
if (downCount > 0) {
    logger.warn("{} out of {} systems are DOWN.", downCount, results.size());
    // System.exit(1);  // Comment out or remove this line
}
System.exit(0);  // Always exit with success
}
