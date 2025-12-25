package com.tearsdeepmind;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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
public class Crawler {

    @Value("${app.login.url}")
    private String loginUrl;

    @Value("${app.email}")
    private String email;

    @Value("${app.password}")
    private String password;

    @Value("${app.driver.path}")
    private String driverPath;

    private WebDriver driver;

    @PostConstruct
    public void startCrawling() {
        System.out.println("Crawler iniciado con Selenium.");
        System.out.println("Usuario: " + email);

        // Create output directory structure
        Path outputPath = createOutputDirectories();
        if (outputPath == null) {
            System.err.println("Error: No se pudieron crear los directorios de salida.");
            return;
        }
        System.out.println("Los resultados se guardarán en: " + outputPath.toAbsolutePath());

        try {
            driver = login(email, password);
            System.out.println("Login exitoso (aparentemente). El crawler continuará aquí.");

            WebDriverWait mainWait = new WebDriverWait(driver, Duration.ofSeconds(20));
            // Wait for the post-login page to stabilize
            System.out.println("Esperando que la página post-login se estabilice...");
            mainWait.until(ExpectedConditions.not(ExpectedConditions.urlContains("sign_in"))); // Wait until not on the sign_in page
            Thread.sleep(5000); // Additional buffer for page load/redirects after login

            // Call the crawling logic
            crawlAndExtractData(driver, outputPath);

            // Espera de 10 segundos para verificación visual
            System.out.println("Esperando 10 segundos antes de cerrar...");
            Thread.sleep(10000);

        } catch (Exception e) {
            System.err.println("Ocurrió un error durante el proceso de login con Selenium o el crawling.");
            e.printStackTrace();
        } finally {
            if (driver != null) {
                System.out.println("Navegador abierto para inspección manual. Cerrando en 60 segundos...");
                try {
                    Thread.sleep(60000); // Mantener el navegador abierto para inspección manual
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                    System.err.println("La espera del navegador fue interrumpida.");
                }
                driver.quit();
                System.out.println("Navegador cerrado.");
            }
        }
    }

    private WebDriver login(String email, String password) throws InterruptedException {
        System.setProperty("webdriver.chrome.driver", driverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        // options.addArguments("--headless"); // Descomentar para ejecutar sin abrir ventana del navegador
        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize(); // Maximizar ventana para asegurar visibilidad de elementos

        driver.get(loginUrl);
        Thread.sleep(2000); // Dar tiempo a que la página cargue completamente y JavaScript se ejecute

        System.out.println("URL actual: " + driver.getCurrentUrl());
        System.out.println("Título de la página: " + driver.getTitle());
        System.out.println("Página de login abierta. Introduciendo credenciales...");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // Aumentar espera máxima si es necesario

        // Esperar a que el campo de email esté presente y sea interactuable
        WebElement emailInput = wait.until(ExpectedConditions.elementToBeClickable(By.name("email")));
        WebElement passwordInput = wait.until(ExpectedConditions.elementToBeClickable(By.name("password")));

        emailInput.sendKeys(email);
        passwordInput.sendKeys(password);

        passwordInput.submit();

        System.out.println("Formulario de login enviado.");

        return driver;
    }

    private Path createOutputDirectories() {
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateFolder = today.format(formatter);

            Path baseDir = Paths.get("TearAnalysis");
            Path dailyDir = baseDir.resolve(dateFolder);

            Files.createDirectories(dailyDir);
            return dailyDir;
        } catch (IOException e) {
            System.err.println("Error al crear los directorios de salida: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void crawlAndExtractData(WebDriver driver, Path outputPath) {
        System.out.println("Iniciando crawling y extracción de datos...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Set<String> visitedThreadUrls = new HashSet<>();

        try {
            // Navigate to the specific section after login
            String sectionUrl = "https://tradingedge.club/spaces/20140826";
            driver.get(sectionUrl);
            System.out.println("Navegando a la sección: " + sectionUrl);
            wait.until(ExpectedConditions.urlContains(sectionUrl));
            Thread.sleep(3000); // Give some time for dynamic content to load

            // 2. Find all potential thread links on the current page
            // Wait for the feed-list container to be visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));

            // Find all 'a' elements within the 'feed-list' that have the class 'feed-item-post'
            List<WebElement> threadLinks = driver.findElements(By.cssSelector("#feed-list a.feed-item-post"));

            System.out.println("Encontrados " + threadLinks.size() + " posibles enlaces a hilos dentro de la sección.");

            int threadsProcessed = 0; // Counter for processed threads
            for (int i = 0; i < threadLinks.size() && threadsProcessed < 2; i++) { // Limit to 2 threads
                // Re-find elements to avoid StaleElementReferenceException
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list"))); // Ensure the list is still visible
                threadLinks = driver.findElements(By.cssSelector("#feed-list a.feed-item-post"));

                if (i >= threadLinks.size()) { // Check if the list size changed during re-finding
                    break;
                }

                WebElement threadLinkElement = threadLinks.get(i);
                String threadUrl = threadLinkElement.getAttribute("href");

                if (threadUrl == null || threadUrl.isEmpty() || visitedThreadUrls.contains(threadUrl)) {
                    continue;
                }

                System.out.println("Visitando hilo: " + threadUrl);
                driver.get(threadUrl);
                visitedThreadUrls.add(threadUrl);

                // Wait for the thread detail page to load
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("body"))); // Wait for body to be visible
                Thread.sleep(2000); // Give some time for content to render

                String threadTitle = "No Title Found";
                String threadContent = "";

                try {
                    // Precise selector for thread title on detail page
                    WebElement titleElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.detail-layout-title h1")));
                    threadTitle = titleElement.getText().trim();
                    System.out.println("Título extraído: " + threadTitle); // Debugging output
                } catch (Exception e) {
                    System.err.println("No se pudo encontrar el título del hilo en: " + threadUrl + ". Usando título por defecto.");
                    threadTitle = "Thread-" + System.currentTimeMillis(); // Fallback title
                }

                try {
                    // Precise selector for main post content on detail page
                    WebElement contentElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.detail-layout-description.mighty-wysiwyg-content.mighty-max-content-width.fr-view")));
                    threadContent = contentElement.getText().trim();
                } catch (Exception e) {
                    System.err.println("No se pudo encontrar el contenido principal del hilo en: " + threadUrl + ". Obteniendo todo el texto visible del cuerpo.");
                    threadContent = driver.findElement(By.cssSelector("body")).getText().trim(); // Fallback to entire body
                }
                
                // Generate filename using only the sanitized thread title as requested
                String fileName = sanitizeFilename(threadTitle) + ".txt";

                Path filePath = outputPath.resolve(fileName);
                try (FileWriter writer = new FileWriter(filePath.toFile(), StandardCharsets.UTF_8)) { // Specify UTF-8 encoding
                    writer.write("URL: " + threadUrl + "\n\n");
                    writer.write("Título: " + threadTitle + "\n\n");
                    writer.write("Contenido:\n" + threadContent);
                    System.out.println("Hilo guardado: " + filePath.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Error al guardar el hilo " + threadTitle + ": " + e.getMessage());
                    e.printStackTrace();
                }

                threadsProcessed++; // Increment the counter
                driver.navigate().back(); // Go back to the list of threads
                wait.until(ExpectedConditions.urlContains(sectionUrl)); // Wait for the section page to load again
                Thread.sleep(2000); // Give some time for the page to render
            }

            System.out.println("Extracción de datos completada.");

        } catch (Exception e) {
            System.err.println("Error durante el crawling: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Helper method to sanitize filenames
    private String sanitizeFilename(String name) {
        // Replace invalid characters for filenames with an underscore
        String sanitized = name.replaceAll("[^a-zA-Z0-9-_.]", "_");
        // Trim to prevent leading/trailing underscores and limit length if necessary
        return sanitized.trim();
    }
}