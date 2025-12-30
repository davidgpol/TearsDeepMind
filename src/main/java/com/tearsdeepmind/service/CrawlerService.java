package com.tearsdeepmind.service;

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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private JobStore jobStore;

    private ExecutorService browserPool;

    @PostConstruct
    public void init() {
        // Pool of 3 parallel browsers for async tasks
        this.browserPool = Executors.newFixedThreadPool(3);
    }

    @PreDestroy
    public void shutdown() {
        if (browserPool != null) browserPool.shutdown();
    }

    // --- EXISTING SYNC METHODS (Preserved) ---

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
            // Check based on today's date for simplicity in "check" mode, or scan recent folders.
            // For checking new threads, we can check if file exists in today's potential folder or just list recent ones.
            // Simplified logic: Check if exists in today's folder.
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

    // --- NEW ASYNC INDUSTRIAL ENGINE ---

    public String startAsyncExtraction(String seccion, int dias) {
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        ExtractionJob job = new ExtractionJob(jobId, seccion, dias);
        jobStore.saveJob(job);

        // Run the main orchestrator in a separate thread
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
            logger.info("[{}] Phase 1: Discovery (Infinite Scroll) started.", uuid);

            masterDriver = login(email, password, uuid);
            masterDriver.get(getSectionUrl(job.getSection()));
            
            List<String> urls = collectUrlsWithScroll(masterDriver, job.getTargetDays(), uuid);
            masterDriver.quit(); // Quit master driver after URL collection
            masterDriver = null;
            
            if (urls.isEmpty() && job.getTargetDays() > 0) {
                logger.warn("[{}] No URLs collected, marking job as FAILED.", uuid);
                job.setStatus(JobStatus.FAILED);
                job.setEndTime(LocalDateTime.now());
                jobStore.saveJob(job);
                return;
            } else if (urls.isEmpty() && job.getTargetDays() == 0) {
                logger.info("[{}] No URLs expected, job completed.", uuid);
                job.setStatus(JobStatus.COMPLETED);
                job.setEndTime(LocalDateTime.now());
                jobStore.saveJob(job);
                return;
            }

            job.setPendingUrls(urls); // Set total URLs after collection
            for (String url : urls) {
                job.getTaskDetails().put(url, new ExtractionJob.ThreadTaskStatus(url));
            }
            job.setTotalThreads(urls.size()); // Set total threads after collection
            logger.info("[{}] Phase 1: Discovery completed. Found {} URLs.", uuid, urls.size());

            job.setStatus(JobStatus.EXTRACTING);
            jobStore.saveJob(job);
            logger.info("[{}] Phase 2: Parallel Extraction started with {} browser workers.", uuid, 3);

            // Path is now determined per thread, not globally for the job
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (String url : urls) {
                futures.add(CompletableFuture.runAsync(() -> processUrlWithRetry(url, job), browserPool));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            job.setStatus(job.getFailedCount() == 0 ? JobStatus.COMPLETED : JobStatus.PARTIALLY_COMPLETED);
            job.setEndTime(LocalDateTime.now());
            jobStore.saveJob(job);
            logger.info("[{}] Job {} finished with status: {}", uuid, job.getJobId(), job.getStatus());

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
        
        // Get current attempt count from job details
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
                
                // Extract Date
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
                
                if (dateStr == null || dateStr.trim().isEmpty()) {
                     logger.warn("[{}] Date string is empty for URL: {}, using today.", job.getJobId(), url);
                     dateStr = LocalDate.now().toString();
                } else {
                     logger.info("[{}] Extracted date string: {}", job.getJobId(), dateStr);
                }
                
                LocalDate threadDate = parseDate(dateStr);
                Path outputPath = getOutputPath(job.getSection(), threadDate);

                String content;
                try {
                    content = workerDriver.findElement(By.cssSelector("div.detail-layout-description")).getText().trim();
                } catch (Exception e) {
                    content = workerDriver.findElement(By.cssSelector("body")).getText().trim();
                }

                Path filePath = outputPath.resolve(sanitizeFilename(title) + ".txt");
                try {
                    Files.write(filePath, ("Title: " + title + "\nDate: " + dateStr + "\nURL: " + url + "\n\n" + content).getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    logger.error("[{}] Failed to write file for URL: {}", job.getJobId(), url, e);
                }
                
                job.markUrlAsCompleted(url);
                jobStore.saveJob(job);
                success = true;
                logger.info("[{}] Successfully processed: {} (Date: {})", job.getJobId(), title, threadDate);

            } catch (Exception e) {
                logger.warn("[{}] Attempt {} failed for URL: {} - Error: {}", job.getJobId(), attempt, url, e.getMessage());
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

    // --- HELPERS ---

    private List<String> collectUrlsWithScroll(WebDriver driver, int target, String uuid) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Set<String> urls = new LinkedHashSet<>();
        int lastSize = 0;
        int noGrowth = 0;

        logger.info("[{}] Starting URL collection scroll loop. Target: {}", uuid, target);

        for (int i = 0; i < 300; i++) { 
            List<WebElement> links = driver.findElements(By.cssSelector("#feed-list a.feed-item-post"));
            for (WebElement l : links) {
                String href = l.getAttribute("href");
                if (href != null) urls.add(href);
            }
            logger.debug("[{}] Scroll cycle {}: Found {} URLs so far. Last size: {}. No growth attempts: {}", uuid, i, urls.size(), lastSize, noGrowth);
            
            if (urls.size() >= target) {
                logger.info("[{}] Target URLs collected ({}). Stopping scroll.", uuid, urls.size());
                break;
            }
            
            if (urls.size() == lastSize) noGrowth++;
            else noGrowth = 0;
            
            if (noGrowth >= 5) { // Increased noGrowth attempts before stopping
                logger.info("[{}] No new URLs found after {} attempts. Stopping scroll.", uuid, noGrowth);
                break;
            }
            
            lastSize = urls.size();
            if (!links.isEmpty()) {
                js.executeScript("arguments[0].scrollIntoView(true);", links.get(links.size() - 1));
            } else {
                 js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            }
            Thread.sleep(5000);
        }
        logger.info("[{}] Finished URL collection. Total URLs found: {}", uuid, urls.size());
        return new ArrayList<>(urls);
    }

    private Path getOutputPath(String seccion, LocalDate date) throws IOException {
        Path path = Paths.get("TearsDeepMind", "TearsMind", date.format(DateTimeFormatter.ofPattern("yyyyMMdd")), seccion);
        Files.createDirectories(path);
        return path;
    }

    private String getSectionUrl(String seccion) {
        if ("DailyAnalysis".equalsIgnoreCase(seccion)) return "https://tradingedge.club/spaces/20140826";
        if ("QuantUpdates".equalsIgnoreCase(seccion)) return "https://tradingedge.club/spaces/20140900/feed";
        return null;
    }

    private WebDriver login(String email, String password, String uuid) throws InterruptedException {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (headless) options.addArguments("--headless=new");
        options.addArguments("--disable-gpu", "--window-size=1920,1080", "--remote-allow-origins=*");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60)); // Explicit page load timeout

        logger.info("[{}] Initializing WebDriver and setting page load timeout.", uuid);

        ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        logger.info("[{}] Injected navigator.webdriver bypass.", uuid);

        driver.get(LOGIN_URL);
        logger.info("[{}] Navigated to login page. Waiting 15 seconds for initial load.", uuid);
        Thread.sleep(15000); 
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        try {
            logger.info("[{}] Waiting for document ready state and login elements.", uuid);
            wait.until(d -> ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
            wait.until(ExpectedConditions.elementToBeClickable(By.name("email"))).sendKeys(email);
            wait.until(ExpectedConditions.elementToBeClickable(By.name("password"))).sendKeys(password);
            wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Iniciar sesión"))).click();
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.not(ExpectedConditions.urlContains("sign_in")));
            logger.info("[{}] Login successful.", uuid);
            return driver;
        } catch (Exception e) {
            logger.error("[{}] Login failed after browser setup: {}", uuid, e.getMessage(), e);
            throw e;
        }
    }

    private void crawlAndExtractDataSync(WebDriver driver, String seccion, String sectionUrl, int dias, String uuid) throws InterruptedException, IOException {
        driver.get(sectionUrl);
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));
        List<String> urls = collectUrlsWithScroll(driver, dias, uuid);
        for (String url : urls.subList(0, Math.min(urls.size(), dias))) {
            driver.get(url);
            Thread.sleep(2000);
            String title = driver.findElement(By.cssSelector("div.detail-layout-title h1")).getText().trim();
            
            String dateStr = "";
            try {
                dateStr = driver.findElement(By.cssSelector(".mighty-attribution-meta span")).getAttribute("title");
            } catch (Exception e) {
                logger.warn("[{}] Could not find date element for URL: {}", uuid, url);
                dateStr = LocalDate.now().toString();
            }
            LocalDate threadDate = parseDate(dateStr);
            Path outputPath = getOutputPath(seccion, threadDate);
            
            String content = driver.findElement(By.cssSelector("body")).getText().trim();
            try {
                Files.write(outputPath.resolve(sanitizeFilename(title) + ".txt"), ("Title: " + title + "\nDate: " + dateStr + "\nURL: " + url + "\n\n" + content).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                logger.error("[{}] Failed to write file for URL: {}", uuid, url, e);
            }
        }
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_.]", "_").trim();
    }
    
    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty()) return LocalDate.now();
            
            // Normalize: "Dic. 1, 2025" -> "dic 1, 2025"
            String cleanDate = dateStr.replace(".", "").toLowerCase().trim();
            
            // Expected format: "mmm d, yyyy"
            // Split by spaces: ["dic", "1,", "2025"]
            String[] parts = cleanDate.split("\\s+");
            if (parts.length < 3) return LocalDate.now();
            
            String monthStr = parts[0];
            int day = Integer.parseInt(parts[1].replace(",", ""));
            int year = Integer.parseInt(parts[2]);
            
            int month;
            switch (monthStr) {
                case "ene": month = 1; break;
                case "feb": month = 2; break;
                case "mar": month = 3; break;
                case "abr": month = 4; break;
                case "may": month = 5; break;
                case "jun": month = 6; break;
                case "jul": month = 7; break;
                case "ago": month = 8; break;
                case "sep": case "sept": month = 9; break;
                case "oct": month = 10; break;
                case "nov": month = 11; break;
                case "dic": month = 12; break;
                default: 
                    logger.warn("Unknown month '{}', defaulting to today.", monthStr);
                    return LocalDate.now();
            }
            
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            logger.warn("Failed to parse date '{}', defaulting to today. Error: {}", dateStr, e.getMessage());
            return LocalDate.now();
        }
    }
}
