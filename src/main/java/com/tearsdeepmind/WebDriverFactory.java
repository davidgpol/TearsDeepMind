package com.tearsdeepmind;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class WebDriverFactory {

    @Value("${app.driver.path}")
    private String driverPath;

    private ChromeOptions options;

    public WebDriverFactory(@Value("${app.driver.path}") String driverPath) {
        this.driverPath = driverPath;
        this.options = createChromeOptions();
    }

    private ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--headless"); // Run in headless mode for server environments
        return options;
    }

    public WebDriver createWebDriver() {
        System.setProperty("webdriver.chrome.driver", Paths.get(driverPath).toAbsolutePath().toString());
        return new ChromeDriver(options);
    }
}
