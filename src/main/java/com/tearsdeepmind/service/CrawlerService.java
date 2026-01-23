package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.CheckReportResponse;
import com.tearsdeepmind.model.ExtractionJob;
import com.tearsdeepmind.model.JobStatus;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CrawlerService {

    private static final Logger logger = LogManager.getLogger(CrawlerService.class);
    private static final String LOGIN_URL = "https://tradingedge.club/sign_in";

    @Value("${crawler.email}")
    private String email;

    @Value("${crawler.password}")
    private String password;

    @Value("${crawler.headless:true}")
    private boolean headless;

    @Value("${crawler.output.dir:/app/TearsMind}")
    private String outputDir;

    @Autowired
    private JobStore jobStore;

    private ExecutorService browserPool;

    @PostConstruct
    public void init() {
        this.browserPool = Executors.newFixedThreadPool(3);
        logger.info("CrawlerService initialized. Output Dir: {}", outputDir);
        logger.info("Credentials configured: {}", (email != null && !email.isEmpty() && password != null && !password.isEmpty()));
    }

    @PreDestroy
    public void shutdown() {
        if (browserPool != null) browserPool.shutdown();
    }

    public CheckReportResponse checkReportExistence(String seccion, LocalDate targetDate) {
        String uuid = UUID.randomUUID().toString();
        String sectionUrl = getSectionUrl(seccion);
        if (sectionUrl == null) return CheckReportResponse.notFound(seccion, targetDate.toString());

        WebDriver driver = null;
        try {
            driver = login(email, password, uuid);
            driver.get(sectionUrl);
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));

            List<WebElement> itemElements = driver.findElements(By.cssSelector("#feed-list a.feed-item-post"));
            List<String> urlsToCheck = new ArrayList<>();
            for (int i = 0; i < Math.min(itemElements.size(), 5); i++) {
                String href = itemElements.get(i).getAttribute("href");
                if (href != null && !href.isEmpty()) {
                    urlsToCheck.add(href);
                }
            }

            for (String url : urlsToCheck) {
                try {
                    driver.get(url);
                    String dateStr = "";
                    String title = "";
                    try {
                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                        title = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.detail-layout-title h1"))).getText().trim();
                        
                        WebElement dateEl = driver.findElement(By.cssSelector(".mighty-attribution-meta span"));
                        dateStr = dateEl.getAttribute("title");
                        if (dateStr == null || dateStr.trim().isEmpty()) {
                            dateStr = dateEl.getText();
                        }
                    } catch (Exception e) {
                        logger.warn("[{}] Could not extract metadata from detail page: {}", uuid, url);
                        continue;
                    }

                    LocalDate postDate = parseDate(dateStr);
                    logger.info("[{}] Checked: {} -> Date: {}", uuid, title, postDate);

                    if (postDate.isEqual(targetDate)) {
                        return new CheckReportResponse(seccion, targetDate.toString(), true, title, url);
                    } 
                } catch (Exception e) {
                    logger.warn("[{}] Error visiting check url: {}", uuid, url);
                }
            }

        } catch (Exception e) {
            logger.error("[{}] Check existence error", uuid, e);
        } finally {
            if (driver != null) driver.quit();
        }
        return CheckReportResponse.notFound(seccion, targetDate.toString());
    }

    @Async
    public CompletableFuture<Void> extract(String seccion, int dias, String uuid) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        WebDriver driver = null;
        try {
            driver = login(email, password, uuid);
            crawlAndExtractDataSync(driver, seccion, getSectionUrl(seccion), dias, uuid);
        } catch (Exception e) {
            logger.error("Sync extract error", e);
        } finally {
            if (driver != null) driver.quit();
        }
        return CompletableFuture.completedFuture(null);
    }

    public List<String> checkForNewThreads(String seccion) {
        String uuid = UUID.randomUUID().toString();
        List<String> newThreads = new ArrayList<>();
        String sectionUrl = getSectionUrl(seccion);
        if (sectionUrl == null || email == null) return newThreads;

        WebDriver driver = null;
        try {
            driver = login(email, password, uuid);
            driver.get(sectionUrl);
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));
            
            Path sectionDir = getOutputPath(seccion, LocalDate.now());
            boolean dirExists = Files.exists(sectionDir);
            List<WebElement> threadLinks = driver.findElements(By.cssSelector("#feed-list a.feed-item-post"));
            for (WebElement link : threadLinks) {
                String title = link.getText().trim();
                if (!title.isEmpty() && (!dirExists || !Files.exists(sectionDir.resolve(sanitizeFilename(title) + ".txt")))) {
                    newThreads.add(title);
                }
            }
        } catch (Exception e) {
            logger.error("Check error", e);
        } finally {
            if (driver != null) driver.quit();
        }
        return newThreads;
    }

    public String startAsyncExtraction(String seccion, int dias) {
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        ExtractionJob job = new ExtractionJob(jobId, seccion, dias);
        jobStore.saveJob(job);
        CompletableFuture.runAsync(() -> orchestrateAsyncJob(job));
        return jobId;
    }

    private void orchestrateAsyncJob(ExtractionJob job) {
        String uuid = job.getJobId();
        logger.info("[{}] Starting orchestrator for job {}", uuid, job.getJobId());
        WebDriver masterDriver = null;

        try {
            job.setStatus(JobStatus.DISCOVERING);
            jobStore.saveJob(job);
            
            masterDriver = login(email, password, uuid);
            masterDriver.get(getSectionUrl(job.getSection()));
            
            List<String> urls = collectUrlsWithScroll(masterDriver, job.getTargetDays(), uuid);
            masterDriver.quit(); 
            masterDriver = null;
            
            if (urls.isEmpty() && job.getTargetDays() > 0) {
                job.setStatus(JobStatus.FAILED);
                job.setEndTime(LocalDateTime.now());
                jobStore.saveJob(job);
                return;
            } else if (urls.isEmpty() && job.getTargetDays() == 0) {
                job.setStatus(JobStatus.COMPLETED);
                job.setEndTime(LocalDateTime.now());
                jobStore.saveJob(job);
                return;
            }

            job.setPendingUrls(urls);
            for (String url : urls) {
                job.getTaskDetails().put(url, new ExtractionJob.ThreadTaskStatus(url));
            }
            job.setTotalThreads(urls.size());

            job.setStatus(JobStatus.EXTRACTING);
            jobStore.saveJob(job);

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String url : urls) {
                futures.add(CompletableFuture.runAsync(() -> processUrlWithRetry(url, job), browserPool));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            job.setStatus(job.getFailedCount() == 0 ? JobStatus.COMPLETED : JobStatus.PARTIALLY_COMPLETED);
            job.setEndTime(LocalDateTime.now());
            jobStore.saveJob(job);

        } catch (Exception e) {
            logger.error("[{}] Fatal error in job orchestrator for job {}", uuid, job.getJobId(), e);
            job.setStatus(JobStatus.FAILED);
            job.setEndTime(LocalDateTime.now());
            jobStore.saveJob(job);
        } finally {
            if (masterDriver != null) masterDriver.quit();
        }
    }

    private void processUrlWithRetry(String url, ExtractionJob job) {
        int maxAttempts = 3;
        ExtractionJob.ThreadTaskStatus taskStatus = job.getTaskDetails().computeIfAbsent(url, k -> new ExtractionJob.ThreadTaskStatus(url));
        int attempt = taskStatus.getRetries();
        boolean success = false;
        
        while (attempt < maxAttempts && !success) {
            attempt++;
            WebDriver workerDriver = null;
            try {
                workerDriver = login(email, password, job.getJobId() + "-worker-" + attempt);
                workerDriver.get(url);
                
                WebDriverWait wait = new WebDriverWait(workerDriver, Duration.ofSeconds(30));
                String title = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.detail-layout-title h1"))).getText().trim();
                
                String dateStr = "";
                try {
                    WebElement dateEl = workerDriver.findElement(By.cssSelector(".mighty-attribution-meta span"));
                    dateStr = dateEl.getAttribute("title");
                    if (dateStr == null || dateStr.trim().isEmpty()) {
                        dateStr = dateEl.getText();
                    }
                } catch (Exception e) {
                    logger.warn("[{}] Could not find date element for URL: {}", job.getJobId(), url);
                }
                
                if (dateStr == null || dateStr.trim().isEmpty()) dateStr = LocalDate.now().toString();
                
                LocalDate threadDate = parseDate(dateStr);
                Path outputPath = getOutputPath(job.getSection(), threadDate);

                String content;
                try {
                    content = workerDriver.findElement(By.cssSelector("div.detail-layout-description")).getText().trim();
                } catch (Exception e) {
                    content = workerDriver.findElement(By.cssSelector("body")).getText().trim();
                }

                Path filePath = outputPath.resolve(sanitizeFilename(title) + ".txt");
                Files.write(filePath, ("Title: " + title + "\nDate: " + dateStr + "\nURL: " + url + "\n\n" + content).getBytes(StandardCharsets.UTF_8));
                
                job.markUrlAsCompleted(url);
                jobStore.saveJob(job);
                success = true;

            } catch (Exception e) {
                if (workerDriver != null) {
                    saveDebugHtml(workerDriver, job.getJobId(), url);
                }
                job.markUrlAsFailed(url, e.getMessage());
                jobStore.saveJob(job);
                if (attempt < maxAttempts) {
                    try { Thread.sleep(5000L * attempt); } catch (InterruptedException ignored) {}
                } 
            } finally {
                if (workerDriver != null) workerDriver.quit();
            }
        }
    }

    private WebDriver login(String email, String password, String uuid) throws InterruptedException, MalformedURLException {
        ChromeOptions options = new ChromeOptions();
        // Stealth & Stability settings
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        // Anti-Detection / WAF Bypass
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation", "enable-logging"));
        options.setExperimentalOption("useAutomationExtension", false);
        
        // Corporate Proxy SSL Bypass
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors=yes");
        options.addArguments("--allow-insecure-localhost");

        String remoteUrl = System.getenv("SELENIUM_REMOTE_URL");
        WebDriver driver;
        
        if (remoteUrl != null && !remoteUrl.isEmpty()) {
            logger.info("[{}] Connecting to Remote Selenium at: {}", uuid, remoteUrl);
            driver = new RemoteWebDriver(new URL(remoteUrl), options);
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("linux")) {
                WebDriverManager.chromedriver().setup();
            }
            if (headless) options.addArguments("--headless=new");
            driver = new ChromeDriver(options);
        }

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(90));
        // JS Bypass for navigator.webdriver
        try {
            ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        } catch (Exception e) {
            logger.warn("[{}] Could not inject JS bypass: {}", uuid, e.getMessage());
        }

        driver.get(LOGIN_URL);
        logger.info("[{}] Navigated to login page. Waiting 15s for Cloudflare challenge...", uuid);
        Thread.sleep(15000); 
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        try {
            // Check if we are stuck on Cloudflare challenge
            String pageTitle = driver.getTitle();
            logger.info("[{}] Page Title: {}", uuid, pageTitle);
            
            wait.until(d -> ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("email")));
            
            // Interaction
            WebElement emailField = wait.until(ExpectedConditions.elementToBeClickable(By.name("email")));
            emailField.clear();
            emailField.sendKeys(email);
            
            WebElement passField = wait.until(ExpectedConditions.elementToBeClickable(By.name("password")));
            passField.clear();
            passField.sendKeys(password);
            
            // SeniorDeveloperAgent: Use CSS selector for submit button to avoid encoding/language issues
            // Usually it's an input of type submit or a button inside the form
            try {
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[type='submit'], button[type='submit']"))).click();
            } catch (Exception e) {
                // Fallback to form submit
                passField.submit();
            }
            
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.not(ExpectedConditions.urlContains("sign_in")));
            logger.info("[{}] Login successful.", uuid);
            return driver;
        } catch (Exception e) {
            logger.error("[{}] Login failed. Title: {}, Error: {}", uuid, driver.getTitle(), e.getMessage());
            if(driver != null) driver.quit();
            throw e;
        }
    }

    private void crawlAndExtractDataSync(WebDriver driver, String seccion, String sectionUrl, int dias, String uuid) throws InterruptedException, IOException {
        driver.get(sectionUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));
        
        List<String> urls = collectUrlsWithScroll(driver, dias, uuid);
        List<String> targetUrls = urls.subList(0, Math.min(urls.size(), dias));
        
        for (String url : targetUrls) {
            try {
                driver.get(url);
                String title = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.detail-layout-title h1"))).getText().trim();
                
                String dateStr = "";
                try {
                    WebElement dateEl = driver.findElement(By.cssSelector(".mighty-attribution-meta span"));
                    dateStr = dateEl.getAttribute("title");
                    if (dateStr == null || dateStr.trim().isEmpty()) {
                        dateStr = dateEl.getText();
                    }
                } catch (Exception e) {
                    logger.warn("[{}] Could not find date element for URL: {}", uuid, url);
                }
                
                if (dateStr == null || dateStr.trim().isEmpty()) dateStr = LocalDate.now().toString();

                LocalDate threadDate = parseDate(dateStr);
                Path outputPath = getOutputPath(seccion, threadDate);
                
                String content;
                try {
                    content = driver.findElement(By.cssSelector("div.detail-layout-description")).getText().trim();
                } catch (Exception e) {
                    content = driver.findElement(By.cssSelector("body")).getText().trim();
                }

                Path filePath = outputPath.resolve(sanitizeFilename(title) + ".txt");
                Files.write(filePath, ("Title: " + title + "\nDate: " + dateStr + "\nURL: " + url + "\n\n" + content).getBytes(StandardCharsets.UTF_8));
                logger.info("[{}] Sync processed: {} (Date: {})", uuid, title, threadDate);

            } catch (Exception e) {
                logger.error("[{}] Sync extraction failed for URL: {}", uuid, url, e.getMessage());
            }
        }
    }

    private List<String> collectUrlsWithScroll(WebDriver driver, int target, String uuid) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Set<String> urls = new LinkedHashSet<>();
        int lastSize = 0;
        int noGrowth = 0;

        for (int i = 0; i < 300; i++) { 
            List<WebElement> items = driver.findElements(By.cssSelector("#feed-list li.feed-item"));
            for (WebElement item : items) {
                try {
                    List<WebElement> postLinks = item.findElements(By.cssSelector("a.feed-item-post"));
                    for (WebElement link : postLinks) {
                        String href = link.getAttribute("href");
                        if (href != null && !href.isEmpty() && !urls.contains(href)) {
                            urls.add(href);
                        }
                    }
                } catch (Exception e) {}
            }

            if (urls.size() >= target) break;
            
            if (urls.size() == lastSize) noGrowth++; else noGrowth = 0;
            if (noGrowth >= 5) break;
            
            lastSize = urls.size();
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(3000);
        }
        return new ArrayList<>(urls);
    }

    private Path getOutputPath(String seccion, LocalDate date) throws IOException {
        Path path = Paths.get(outputDir, date.format(DateTimeFormatter.ofPattern("yyyyMMdd")), seccion);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    private String getSectionUrl(String seccion) {
        if ("DailyAnalysis".equalsIgnoreCase(seccion)) return "https://tradingedge.club/spaces/20140826";
        if ("QuantUpdates".equalsIgnoreCase(seccion)) return "https://tradingedge.club/spaces/20140900/feed";
        return null;
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_.]", "_").trim();
    }
    
    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty()) return LocalDate.now();
            String cleanDate = dateStr.replace(".", "").toLowerCase().trim();
            // Remove "th", "st", "nd", "rd" from day numbers if present (e.g. "Jan 1st")
            cleanDate = cleanDate.replaceAll("(\\d+)(st|nd|rd|th)", "$1");
            
            String[] parts = cleanDate.split("\\s+");
            if (parts.length < 3) return LocalDate.now();
            
            String monthStr = parts[0];
            int day = Integer.parseInt(parts[1].replace(",", ""));
            int year = Integer.parseInt(parts[2]);
            
            int month;
            switch (monthStr) {
                case "ene": case "jan": month = 1; break;
                case "feb":             month = 2; break;
                case "mar":             month = 3; break;
                case "abr": case "apr": month = 4; break;
                case "may":             month = 5; break;
                case "jun":             month = 6; break;
                case "jul":             month = 7; break;
                case "ago": case "aug": month = 8; break;
                case "sep": case "sept": month = 9; break;
                case "oct":             month = 10; break;
                case "nov":             month = 11; break;
                case "dic": case "dec": month = 12; break;
                default: return LocalDate.now();
            }
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            logger.warn("Date parse error for input '{}': {}", dateStr, e.getMessage());
            return LocalDate.now();
        }
    }

    private void saveDebugHtml(WebDriver driver, String jobId, String url) {
        try {
            Path debugDir = Paths.get(outputDir, "debug");
            if (!Files.exists(debugDir)) {
                Files.createDirectories(debugDir);
            }
            String filename = jobId + "_" + sanitizeFilename(url) + ".html";
            Path debugFile = debugDir.resolve(filename);
            Files.write(debugFile, driver.getPageSource().getBytes(StandardCharsets.UTF_8));
            logger.info("Saved debug HTML to: {}", debugFile);
        } catch (Exception ex) {
            logger.error("Failed to save debug HTML", ex);
        }
    }
}