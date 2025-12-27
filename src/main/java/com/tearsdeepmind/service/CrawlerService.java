package com.tearsdeepmind.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CrawlerService {

    private static final Logger logger = LogManager.getLogger(CrawlerService.class);
    private static final String LOGIN_URL = "https://tradingedge.club/sign_in";

    @Value("${crawler.email}")
    private String email;

    @Value("${crawler.password}")
    private String password;

    public void extract(String seccion, int dias, String uuid) {
        logger.info("[{}] Starting extraction for section {} and days {}", uuid, seccion, dias);
        
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            logger.error("[{}] Email or password are not configured.", uuid);
            return;
        }

        WebDriver driver = null;
        try {
            driver = login(email, password, uuid);
            logger.info("[{}] Login successful (apparently). The crawler will continue here.", uuid);

            Path dailyDir = createOutputDirectories(seccion, uuid);
            if (dailyDir == null) {
                logger.error("[{}] Could not create output directories.", uuid);
                return;
            }

            crawlAndExtractData(driver, dailyDir, getSectionUrl(seccion), dias, uuid);

        } catch (Exception e) {
            logger.error("[{}] An error occurred during the Selenium login or crawling process.", uuid, e);
        } finally {
            if (driver != null) {
                driver.quit();
                logger.info("[{}] Browser closed.", uuid);
            }
        }
    }

    public boolean check(String seccion) {
        // This is a placeholder for testing purposes.
        // In a real scenario, this would involve comparing current threads with previously stored ones.
        return "DailyAnalysis".equalsIgnoreCase(seccion);
    }

    private String getSectionUrl(String seccion) {
        if ("DailyAnalysis".equalsIgnoreCase(seccion)) {
            return "https://tradingedge.club/spaces/20140826";
        } else if ("QuantUpdates".equalsIgnoreCase(seccion)) {
            return "https://tradingedge.club/spaces/20140900/feed";
        }
        return null;
    }

    private WebDriver login(String email, String password, String uuid) throws InterruptedException {
        logger.info("[{}] Setting up web driver.", uuid);
        
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        
        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        driver.get(LOGIN_URL);
        logger.info("[{}] Opened login page: {}", uuid, LOGIN_URL);
        Thread.sleep(2000);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        WebElement emailInput = wait.until(ExpectedConditions.elementToBeClickable(By.name("email")));
        WebElement passwordInput = wait.until(ExpectedConditions.elementToBeClickable(By.name("password")));

        emailInput.sendKeys(email);
        passwordInput.sendKeys(password);
        logger.info("[{}] Entered credentials.", uuid);

        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(), 'Iniciar sesión')]")));
        loginButton.click();
        logger.info("[{}] Clicked login button.", uuid);

        WebDriverWait postLoginWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            postLoginWait.until(ExpectedConditions.not(ExpectedConditions.urlContains("sign_in")));
            logger.info("[{}] Login successful, redirected from login page.", uuid);
        } catch (TimeoutException e) {
            logger.error("[{}] Login failed: URL did not change after 30 seconds. Still on the login page.", uuid, e);
            throw e;
        }
        return driver;
    }

    private Path createOutputDirectories(String seccion, String uuid) {
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateFolder = today.format(formatter);

            Path baseDir = Paths.get("TearsDeepMind", "TearsMind");
            Path dailyDir = baseDir.resolve(dateFolder);
            Path sectionDir = dailyDir.resolve(seccion);

            Files.createDirectories(sectionDir);
            logger.info("[{}] Created output directory: {}", uuid, sectionDir.toAbsolutePath());
            return sectionDir;
        } catch (IOException e) {
            logger.error("[{}] Error creating output directories.", uuid, e);
            return null;
        }
    }

    private void crawlAndExtractData(WebDriver driver, Path outputPath, String sectionUrl, int dias, String uuid) {
        logger.info("[{}] Starting crawling and data extraction for section: {}", uuid, sectionUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Set<String> visitedThreadUrls = new HashSet<>();

        try {
            logger.info("[{}] Navigating to main section: {}", uuid, sectionUrl);
            driver.get(sectionUrl);

            wait.until(ExpectedConditions.urlContains(sectionUrl));
            
            // Wait for the feed list and at least one post to be visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#feed-list a.feed-item-post")));
            
            Thread.sleep(3000);

            List<WebElement> threadLinks = driver.findElements(By.cssSelector("#feed-list a.feed-item-post"));

            logger.info("[{}] Found {} possible thread links in the section.", uuid, threadLinks.size());

            int threadsProcessed = 0;
            for (int i = 0; i < threadLinks.size() && threadsProcessed < dias; i++) {
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));
                threadLinks = driver.findElements(By.cssSelector("#feed-list a.feed-item-post"));

                if (i >= threadLinks.size()) {
                    break;
                }

                WebElement threadLinkElement = threadLinks.get(i);
                String threadUrl = threadLinkElement.getAttribute("href");

                if (threadUrl == null || threadUrl.isEmpty() || visitedThreadUrls.contains(threadUrl)) {
                    continue;
                }

                logger.info("[{}] Visiting thread: {}", uuid, threadUrl);
                driver.get(threadUrl);
                visitedThreadUrls.add(threadUrl);

                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("body")));
                Thread.sleep(2000);

                String threadTitle = "No Title Found";
                String threadContent = "";

                try {
                    WebElement titleElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.detail-layout-title h1")));
                    threadTitle = titleElement.getText().trim();
                    logger.info("[{}] Extracted title: {}", uuid, threadTitle);
                } catch (Exception e) {
                    logger.warn("[{}] Could not find thread title at {}. Using default title.", uuid, threadUrl);
                    threadTitle = "Thread-" + System.currentTimeMillis();
                }

                try {
                    WebElement contentElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.detail-layout-description.mighty-wysiwyg-content.mighty-max-content-width.fr-view")));
                    threadContent = contentElement.getText().trim();
                } catch (Exception e) {
                    logger.warn("[{}] Could not find main content of the thread at {}. Getting all visible body text.", uuid, threadUrl);
                    threadContent = driver.findElement(By.cssSelector("body")).getText().trim();
                }

                String fileName = sanitizeFilename(threadTitle) + ".txt";

                Path filePath = outputPath.resolve(fileName);
                try (FileWriter writer = new FileWriter(filePath.toFile(), StandardCharsets.UTF_8)) {
                    writer.write("URL: " + threadUrl + "\n\n");
                    writer.write("Título: " + threadTitle + "\n\n");
                    writer.write("Contenido:\n" + threadContent);
                    logger.info("[{}] Saved thread: {}", uuid, filePath.toAbsolutePath());
                } catch (IOException e) {
                    logger.error("[{}] Error saving thread {}", uuid, threadTitle, e);
                }

                threadsProcessed++;
                driver.navigate().back();
                wait.until(ExpectedConditions.urlContains(sectionUrl));
                Thread.sleep(2000);
            }

            logger.info("[{}] Data extraction completed.", uuid);

        } catch (Exception e) {
            logger.error("[{}] Error during crawling.", uuid, e);
        }
    }

    private String sanitizeFilename(String name) {
        String sanitized = name.replaceAll("[^a-zA-Z0-9-_.]", "_");
        return sanitized.trim();
    }
}