package com.hackathonproject.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import com.hackathonproject.utils.ConfigReader;

import java.time.Duration;

public class BaseClass {

    private static final Logger logger = LogManager.getLogger(BaseClass.class);

    // ThreadLocal<T> is a Java utility that gives each thread its own private copy of a variable.
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public static String getCurrentBrowser() {
        String sysProp = System.getProperty("browser"); // -Dbrowser=chrome from Jenkins
        if (sysProp != null && !sysProp.isEmpty()) return sysProp;

        return ConfigReader.getProperty("browser");     // config.properties default
    }

    private static boolean isHeadless() {
        return "true".equalsIgnoreCase(System.getProperty("headless", "false"));
    }

    public static void createDriver() {
        String browser = getCurrentBrowser();
        boolean headless = isHeadless();
        logger.info("Launching browser: " + browser
                + " | headless=" + headless
                + " | thread=" + Thread.currentThread().getName());

        WebDriver webDriver;

        if (browser.equalsIgnoreCase("chrome") || browser.equalsIgnoreCase("edge")) {
            // ✅ EDGE REPLACED WITH CHROME HERE

            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("--start-maximized");
            chromeOptions.addArguments("--disable-notifications");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");

            if (headless) {
                chromeOptions.addArguments("--headless=new");
                chromeOptions.addArguments("--window-size=1920,1080");
                chromeOptions.addArguments("--disable-gpu");
                logger.info("Chrome running in HEADLESS mode");
            }

            WebDriverManager.chromedriver().setup();
            webDriver = new ChromeDriver(chromeOptions);

            logger.info("Chrome browser launched");

        } else if (browser.equalsIgnoreCase("firefox")) {

            FirefoxOptions firefoxOptions = new FirefoxOptions();
            if (headless) {
                firefoxOptions.addArguments("--headless");
                firefoxOptions.addArguments("--width=1920");
                firefoxOptions.addArguments("--height=1080");
                logger.info("Firefox running in HEADLESS mode");
            }

            WebDriverManager.firefoxdriver().setup();
            webDriver = new FirefoxDriver(firefoxOptions);

            logger.info("Firefox browser launched");

        } else {
            throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        webDriver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(Integer.parseInt(ConfigReader.getProperty("implicitWait")))
        );
        webDriver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(Integer.parseInt(ConfigReader.getProperty("maxWaitTime")))
        );

        driver.set(webDriver);

        logger.info("WebDriver [" + browser + "] created and stored in ThreadLocal. Session: "
                + ((org.openqa.selenium.remote.RemoteWebDriver) webDriver).getSessionId());
    }

    public static WebDriver getDriver() {
        return driver.get();
    }

    public static void removeDriver() {
        if (driver.get() != null) {
            String browser = getCurrentBrowser();
            logger.info("Quitting " + browser + " browser on thread: " + Thread.currentThread().getName());
            try {
                driver.get().quit();
            } catch (Exception e) {
                logger.warn("Error quitting browser: " + e.getMessage());
            }
            driver.remove(); // clears ThreadLocal slot
        }
    }
}