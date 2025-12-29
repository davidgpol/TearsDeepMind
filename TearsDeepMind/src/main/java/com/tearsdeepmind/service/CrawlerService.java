package com.tearsdeepmind.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CrawlerService {

    private static final Logger logger = LogManager.getLogger(CrawlerService.class);
    private static final String LOGIN_URL = "https://tradingedge.club/sign_in";

    @Value("${crawler.email}")
    private String email;

    @Value("${crawler.password}")
    private String password;

    @Async
    public CompletableFuture<Void> extract(String seccion, int dias, String uuid) {
        logger.info("[{}] Starting extraction for section {} and days {}", uuid, seccion, dias);
        
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            logger.error("[{}] Email or password are not configured.", uuid);
            return CompletableFuture.completedFuture(null);
        }

        WebDriver driver = null;
        try {
            driver = login(email, password, uuid);
            logger.info("[{}] Login successful (apparently). The crawler will continue here.", uuid);

            Path baseDir = Paths.get("TearsDeepMind", "TearsMind");
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateFolder = today.format(formatter);
            Path dailyDir = baseDir.resolve(dateFolder);
            Path sectionDir = dailyDir.resolve(seccion);
            
            Files.createDirectories(sectionDir);
            logger.info("[{}] Created output directory: {}", uuid, sectionDir.toAbsolutePath());

            crawlAndExtractData(driver, sectionDir, getSectionUrl(seccion), dias, uuid);

        } catch (Exception e) {
            logger.error("[{}] An error occurred during the Selenium login or crawling process.", uuid, e);
        } finally {
            if (driver != null) {
                driver.quit();
                logger.info("[{}] Browser closed.", uuid);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public List<String> checkForNewThreads(String seccion) {
        String uuid = UUID.randomUUID().toString();
        logger.info("[{}] Checking for new threads in section: {}", uuid, seccion);
        List<String> newThreads = new ArrayList<>();

        String sectionUrl = getSectionUrl(seccion);
        if (sectionUrl == null) {
            logger.error("[{}] Invalid section: {}", uuid, seccion);
            return newThreads;
        }

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            logger.error("[{}] Email or password are not configured.", uuid);
            return newThreads;
        }

        WebDriver driver = null;
        try {
            driver = login(email, password, uuid);
            
            logger.info("[{}] Navigating to section feed: {}", uuid, sectionUrl);
            driver.get(sectionUrl);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));
            // Wait for at least one item to ensure feed is loaded
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#feed-list a.feed-item-post")));
            
            // Get today's directory path to check against
            Path baseDir = Paths.get("TearsDeepMind", "TearsMind");
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateFolder = today.format(formatter);
            Path sectionDir = baseDir.resolve(dateFolder).resolve(seccion);

            // If the directory doesn't exist, everything is effectively "new" (or not downloaded yet)
            boolean dirExists = Files.exists(sectionDir);

            List<WebElement> threadLinks = driver.findElements(By.cssSelector("#feed-list a.feed-item-post"));
            logger.info("[{}] Found {} items in feed to check.", uuid, threadLinks.size());

            for (WebElement link : threadLinks) {
                try {
                    // Try to get the title. The structure can vary, but usually the text of the link or a child div contains it.
                    // We'll try to find a specific title class first, fallback to link text.
                    String title = link.getText().trim();
                    if (title.isEmpty()) {
                         // Sometimes title is nested in a specific way or hidden
                         WebElement titleElement = link.findElement(By.xpath(".//*[contains(@class, 'title')]"));
                         title = titleElement.getText().trim();
                    }

                    if (title.isEmpty()) continue;
                    
                    if (!dirExists) {
                        newThreads.add(title);
                        continue;
                    }

                    String expectedFilename = sanitizeFilename(title) + ".txt";
                    Path expectedPath = sectionDir.resolve(expectedFilename);
                    
                    if (!Files.exists(expectedPath)) {
                        newThreads.add(title);
                        logger.debug("[{}] Identified new thread: {}", uuid, title);
                    }
                } catch (Exception e) {
                    logger.warn("[{}] Failed to parse a thread item title during check.", uuid);
                }
            }
            
        } catch (Exception e) {
            logger.error("[{}] Error checking for new threads.", uuid, e);
        } finally {
            if (driver != null) {
                driver.quit();
                logger.info("[{}] Browser closed (Check operation).", uuid);
            }
        }
        
        logger.info("[{}] Check completed. Found {} new threads.", uuid, newThreads.size());
        return newThreads;
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
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
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
    
    // Reuse extraction logic but keeping it encapsulated
    private void crawlAndExtractData(WebDriver driver, Path outputPath, String sectionUrl, int dias, String uuid) {
        logger.info("[{}] Starting crawling and data extraction for section: {}", uuid, sectionUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Set<String> visitedThreadUrls = new HashSet<>();

        try {
            logger.info("[{}] Navigating to main section: {}", uuid, sectionUrl);
            driver.get(sectionUrl);

            wait.until(ExpectedConditions.urlContains(sectionUrl));
            
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