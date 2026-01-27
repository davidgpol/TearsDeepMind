package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.CheckReportResponse;
import com.tearsdeepmind.model.ExtractionJob;
import com.tearsdeepmind.model.JobStatus;
import com.tearsdeepmind.model.SessionContext;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
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
    private static final String BASE_URL = "https://tradingedge.club";
    private static final String LOGIN_URL = BASE_URL + "/sign_in";

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

    @Autowired
    private MonitoringService monitoringService;

    private ExecutorService browserPool;
    private final java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(5);

    @PostConstruct
    public void init() {
        this.browserPool = Executors.newVirtualThreadPerTaskExecutor();
        logger.info("CrawlerService initialized with Virtual Threads. Output Dir: {}", outputDir);
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
        CompletableFuture.runAsync(() -> orchestrateAsyncJob(job), browserPool);
        return jobId;
    }

    private void orchestrateAsyncJob(ExtractionJob job) {
        String uuid = job.getJobId();
        logger.info("[{}] Starting Industrial Orchestrator for job {}", uuid, job.getJobId());
        WebDriver masterDriver = null;

        try {
            job.setStatus(JobStatus.DISCOVERING);
            jobStore.saveJob(job);
            
            masterDriver = login(email, password, uuid);
            monitoringService.publish(new com.tearsdeepmind.model.CrawlerEvent(uuid, "SESSION_INIT", null, 0, 0, "Master Login successful. Capturing session..."));

            // Capture Session Context for sharing
            Set<Cookie> cookies = masterDriver.manage().getCookies();
            String localStorage = (String) ((JavascriptExecutor) masterDriver).executeScript("return JSON.stringify(localStorage);");
            SessionContext session = new SessionContext(cookies, localStorage);
            logger.info("[{}] Session captured. Reusing for workers.", uuid);

            masterDriver.get(getSectionUrl(job.getSection()));
            
            List<String> urls = collectUrlsWithScroll(masterDriver, job.getTargetDays(), uuid);
            masterDriver.quit(); 
            masterDriver = null;
            
            if (urls.isEmpty() && job.getTargetDays() > 0) {
                job.setStatus(JobStatus.FAILED);
                job.setEndTime(LocalDateTime.now());
                jobStore.saveJob(job);
                monitoringService.publish(new com.tearsdeepmind.model.CrawlerEvent(uuid, "JOB_FAILED", null, 0, 0, "No URLs discovered."));
                return;
            } else if (urls.isEmpty() && job.getTargetDays() == 0) {
                job.setStatus(JobStatus.COMPLETED);
                job.setEndTime(LocalDateTime.now());
                jobStore.saveJob(job);
                monitoringService.publish(new com.tearsdeepmind.model.CrawlerEvent(uuid, "JOB_FINISHED", null, 0, 0, "No items to extract."));
                return;
            }

            job.setPendingUrls(urls);
            for (String url : urls) {
                job.getTaskDetails().put(url, new ExtractionJob.ThreadTaskStatus(url));
            }
            job.setTotalThreads(urls.size());

            job.setStatus(JobStatus.EXTRACTING);
            jobStore.saveJob(job);
            monitoringService.publish(new com.tearsdeepmind.model.CrawlerEvent(uuid, "JOB_STARTED", null, 0, urls.size(), "Discovery complete. Starting parallel downloads."));

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String url : urls) {
                futures.add(CompletableFuture.runAsync(() -> processUrlWithSession(url, session, job), browserPool));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            job.setStatus(job.getFailedCount() == 0 ? JobStatus.COMPLETED : JobStatus.PARTIALLY_COMPLETED);
            job.setEndTime(LocalDateTime.now());
            jobStore.saveJob(job);
            monitoringService.publish(new com.tearsdeepmind.model.CrawlerEvent(uuid, "JOB_FINISHED", null, job.getCompletedCount(), job.getTotalThreads(), "Job finished with status: " + job.getStatus()));

        } catch (Exception e) {
            logger.error("[{}] Fatal error in Industrial Orchestrator", uuid, e);
            job.setStatus(JobStatus.FAILED);
            job.setEndTime(LocalDateTime.now());
            jobStore.saveJob(job);
            monitoringService.publish(new com.tearsdeepmind.model.CrawlerEvent(uuid, "JOB_FAILED", null, 0, 0, e.getMessage()));
        } finally {
            if (masterDriver != null) masterDriver.quit();
        }
    }

    private void processUrlWithSession(String url, SessionContext session, ExtractionJob job) {
        int maxAttempts = 3;
        ExtractionJob.ThreadTaskStatus taskStatus = job.getTaskDetails().computeIfAbsent(url, k -> new ExtractionJob.ThreadTaskStatus(url));
        int attempt = taskStatus.getRetries();
        boolean success = false;
        
        while (attempt < maxAttempts && !success) {
            attempt++;
            WebDriver workerDriver = null;
            try {
                semaphore.acquire();
                logger.info("[{}] Worker slot acquired. Starting download: {}", job.getJobId(), url);
                workerDriver = createDriverWithSession(session, job.getJobId() + "-worker-" + attempt);
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
                } catch (Exception e) {}
                
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
                monitoringService.publish(new com.tearsdeepmind.model.CrawlerEvent(job.getJobId(), "ITEM_COMPLETED", title, job.getCompletedCount(), job.getTotalThreads(), "Downloaded successfully."));
                success = true;

            } catch (Exception e) {
                if (workerDriver != null) saveDebugHtml(workerDriver, job.getJobId(), url);
                job.markUrlAsFailed(url, e.getMessage());
                jobStore.saveJob(job);
                if (attempt < maxAttempts) {
                    try { Thread.sleep(2000L * attempt); } catch (InterruptedException ignored) {}
                } 
            } finally {
                if (workerDriver != null) workerDriver.quit();
                semaphore.release();
            }
        }
    }

    private WebDriver createDriverWithSession(SessionContext session, String uuid) throws MalformedURLException {
        WebDriver driver = createBaseDriver(uuid);
        // Navigate to domain first to set cookies
        driver.get(BASE_URL + "/robots.txt"); 
        for (Cookie cookie : session.cookies()) {
            driver.manage().addCookie(cookie);
        }
        // Inject LocalStorage
        if (session.localStorageJson() != null) {
            ((JavascriptExecutor) driver).executeScript(
                "var data = JSON.parse(arguments[0]);" +
                "for (var key in data) { localStorage.setItem(key, data[key]); }", 
                session.localStorageJson()
            );
        }
        return driver;
    }

    private WebDriver createBaseDriver(String uuid) throws MalformedURLException {
        ChromeOptions options = getCommonOptions();
        String remoteUrl = System.getenv("SELENIUM_REMOTE_URL");
        if (remoteUrl != null && !remoteUrl.isEmpty()) {
            logger.info("[{}] Connecting to Remote Selenium at: {}", uuid, remoteUrl);
            return new RemoteWebDriver(new URL(remoteUrl), options);
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("linux")) WebDriverManager.chromedriver().setup();
            if (headless) options.addArguments("--headless=new");
            return new ChromeDriver(options);
        }
    }

    private ChromeOptions getCommonOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu", "--window-size=1920,1080", "--remote-allow-origins=*", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation", "enable-logging"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--ignore-certificate-errors", "--ignore-ssl-errors=yes", "--allow-insecure-localhost");
        return options;
    }

    private WebDriver login(String email, String password, String uuid) throws InterruptedException, MalformedURLException {
        logger.info("[{}] --- Iniciando proceso de login (Flujo de 2 pasos) ---", uuid);
        WebDriver driver = createBaseDriver(uuid);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(90));
        
        try {
            ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        } catch (Exception e) {
            logger.warn("[{}] No se pudo ocultar el flag del WebDriver.", uuid);
        }

        logger.info("[{}] Navegando a: {}", uuid, LOGIN_URL);
        driver.get(LOGIN_URL);
        
        // Espera inicial para Cloudflare o carga de scripts base
        Thread.sleep(15000); 
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        try {
            wait.until(d -> ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
            
            // --- PASO 1: Email ---
            logger.info("[{}] Paso 1: Introduciendo email...", uuid);
            WebElement emailField = wait.until(ExpectedConditions.elementToBeClickable(By.name("email")));
            emailField.clear();
            emailField.sendKeys(email);
            
            logger.info("[{}] Haciendo clic en 'Siguiente' para el email...", uuid);
            WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.mighty-btn-filled-theme-color-button")));
            nextButton.click();
            
            // --- PASO 2: Password ---
            logger.info("[{}] Paso 2: Esperando a que el campo de contraseña sea visible...", uuid);
            WebElement passField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("password")));
            logger.info("[{}] Campo de contraseña detectado. Introduciendo password...", uuid);
            passField.clear();
            passField.sendKeys(password);
            
            logger.info("[{}] Haciendo clic en 'Siguiente' para finalizar login...", uuid);
            // Re-localizamos el botón ya que el DOM puede haber cambiado tras el primer clic
            WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.mighty-btn-filled-theme-color-button")));
            submitButton.click();
            
            // --- PASO 3: Validación ---
            logger.info("[{}] Validando redirección post-login...", uuid);
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("sign_in")));
            
            logger.info("[{}] Login completado con éxito.", uuid);
            return driver;
        } catch (Exception e) {
            logger.error("[{}] >>> ERROR CRÍTICO EN LOGIN: {}", uuid, e.getMessage());
            saveDebugHtml(driver, uuid, "login_failure");
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

        logger.info("[{}] Starting discovery. Target: {} URLs.", uuid, target);

        for (int i = 0; i < 100; i++) { 
            // Broaden search to find any post link in the feed area
            List<WebElement> postLinks = driver.findElements(By.cssSelector("a.feed-item-post, a[href*='/posts/']"));
            for (WebElement link : postLinks) {
                try {
                    String href = link.getAttribute("href");
                    if (href != null && href.contains("/posts/") && !href.contains("/comments") && !urls.contains(href)) {
                        urls.add(href);
                    }
                } catch (Exception e) {}
            }

            logger.info("[{}] Discovery iteration {}: Found {} unique URLs so far.", uuid, i, urls.size());

            if (urls.size() >= target) {
                logger.info("[{}] Discovery target reached.", uuid);
                break;
            }
            
            if (urls.size() == lastSize) {
                noGrowth++;
            } else {
                noGrowth = 0;
            }

            if (noGrowth >= 10) { // Increased tolerance
                logger.warn("[{}] Discovery stopped: No growth in URLs after 10 scrolls.", uuid);
                break;
            }
            
            lastSize = urls.size();
            
            // Refined Scroll: Find the last discovered post and scroll it into view
            if (!postLinks.isEmpty()) {
                try {
                    WebElement lastElement = postLinks.get(postLinks.size() - 1);
                    js.executeScript("arguments[0].scrollIntoView(true);", lastElement);
                    logger.info("[{}] Scrolled to last element found.", uuid);
                } catch (Exception e) {
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                }
            } else {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            }

            Thread.sleep(3000);
            
            if (noGrowth > 5 && urls.size() < target) {
                // Emergency save of discovery page structure
                saveDebugHtml(driver, uuid, "discovery_failure_at_" + urls.size());
            }
        }
        
        List<String> result = new ArrayList<>(urls);
        return result.size() > target ? result.subList(0, target) : result;
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