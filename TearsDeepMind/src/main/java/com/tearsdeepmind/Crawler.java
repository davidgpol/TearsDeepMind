package com.tearsdeepmind;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.TimeoutException;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class Crawler {

    private static final String LOGIN_URL = "https://tradingedge.club/sign_in";

    public static void main(String[] args) {
        Properties props = loadProperties();
        if (props == null) {
            System.err.println("Error: No se pudo cargar el archivo .env con las credenciales.");
            return;
        }

        String email = props.getProperty("EMAIL");
        String password = props.getProperty("PASSWORD");
        String driverPath = props.getProperty("DRIVER_PATH"); // Load driver path from .env

        if (driverPath == null || driverPath.isEmpty()) {
            System.err.println("Error: DRIVER_PATH no está configurado en el archivo .env");
            return;
        }

        System.out.println("Crawler iniciado con Selenium.");
        System.out.println("Usuario: " + email);

        // Create output directory structure
        Path dailyDir = createOutputDirectories();
        if (dailyDir == null) {
            System.err.println("Error: No se pudieron crear los directorios de salida.");
            return;
        }

        Path dailyAnalysisPath = dailyDir.resolve("DailyAnalysis");
        Path quantUpdatesPath = dailyDir.resolve("QuantUpdates");

        System.out.println("Los resultados de DailyAnalysis se guardarán en: " + dailyAnalysisPath.toAbsolutePath());
        System.out.println("Los resultados de QuantUpdates se guardarán en: " + quantUpdatesPath.toAbsolutePath());


        String dailyAnalysisUrl = "https://tradingedge.club/spaces/20140826";
        String quantUpdatesUrl = "https://tradingedge.club/spaces/20140900/feed";

        WebDriver driver = null;
        try {
            driver = login(email, password, driverPath);
            System.out.println("Login exitoso (aparentemente). El crawler continuará aquí.");

            // Call the crawling logic for both sections
            crawlAndExtractData(driver, dailyAnalysisPath, dailyAnalysisUrl);
            crawlAndExtractData(driver, quantUpdatesPath, quantUpdatesUrl);

            // Espera de 10 segundos para verificación visual
            System.out.println("Esperando 10 segundos antes de cerrar...");
            Thread.sleep(10000);

        } catch (Exception e) {
            System.err.println("Ocurrió un error durante el proceso de login con Selenium o el crawling.");
            e.printStackTrace();
        } finally {
            if (driver != null) {
                System.out.println("Navegador abierto para inspección manual. Por favor, examine la página y cierre la ventana del navegador cuando termine.");
                try {
                    // Keep the browser open indefinitely until the user closes it manually or after a very long time
                    // Or, for testing purposes, keep it open for 5 minutes (300 seconds)
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("La espera del navegador fue interrumpida.");
                }
                driver.quit();
                System.out.println("Navegador cerrado.");
            }
        }
    }            
            private static WebDriver login(String email, String password, String driverPath) throws InterruptedException {
                System.setProperty("webdriver.chrome.driver", driverPath);
                
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--disable-blink-features=AutomationControlled");
                options.addArguments("--disable-extensions");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--no-sandbox");
                // options.addArguments("--headless"); // Descomentar para ejecutar sin abrir ventana del navegador
                WebDriver driver = new ChromeDriver(options);
                driver.manage().window().maximize(); // Maximizar ventana para asegurar visibilidad de elementos
            
                driver.get(LOGIN_URL);
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
                
                                
                
                                        // Find and click the login button which is an <a> tag with "Iniciar sesión" text
                
                                        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(), 'Iniciar sesión')]")));
                
                                        loginButton.click();
                
                                        
                
                                                        System.out.println("Formulario de login enviado (haciendo clic en el botón 'Iniciar sesión').");
                
                                        
                
                                                
                
                                        
                
                                                        // This wait is now inside the login method, so the main method doesn't need to do it.
                
                                        
                
                                                        WebDriverWait postLoginWait = new WebDriverWait(driver, Duration.ofSeconds(30)); 
                
                                        
                
                                                        try {
                
                                        
                
                                                            postLoginWait.until(ExpectedConditions.not(ExpectedConditions.urlContains("sign_in")));
                
                                        
                
                                                            System.out.println("Redirección post-login detectada.");
                
                                        
                
                                                        } catch (TimeoutException e) {
                
                                        
                
                                                            // If the URL still contains "sign_in", it means login failed.
                
                                        
                
                                                            // We can try to find an error message or just throw the exception again.
                
                                        
                
                                                            System.err.println("Error de login: La URL no cambió después de 30 segundos. Todavía en la página de login.");
                
                                        
                
                                                            throw e; // Re-throw the exception to indicate login failure
                
                                        
                
                                                        }
                
                                        
                
                                        
            
                return driver;
            }
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(".env")) {
            properties.load(input);
            return properties;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static Path createOutputDirectories() {
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateFolder = today.format(formatter);

            Path baseDir = Paths.get("TearsMind");
            Path dailyDir = baseDir.resolve(dateFolder);

            Files.createDirectories(dailyDir.resolve("DailyAnalysis"));
            Files.createDirectories(dailyDir.resolve("QuantUpdates"));

            return dailyDir;
        } catch (IOException e) {
            System.err.println("Error al crear los directorios de salida: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private static void crawlAndExtractData(WebDriver driver, Path outputPath, String sectionUrl) {
        System.out.println("Iniciando crawling y extracción de datos para la sección: " + sectionUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Set<String> visitedThreadUrls = new HashSet<>();

        try {
            
            // First, ensure we've landed somewhere after login (not still on sign_in).
            // The login method handles this wait.
            
            // Now, explicitly navigate to the desired section URL.
            System.out.println("Navegando a la sección principal: " + sectionUrl);
            driver.get(sectionUrl); 
            
            // Wait for the URL to eventually contain the sectionUrl, allowing for intermediate redirects.
            // First, wait for the URL to not contain "landing" if it was redirected there.
            try {
                wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("landing")));
            } catch (TimeoutException e) {
                System.out.println("La URL sigue conteniendo 'landing' después de la espera, o no se encontró la página de aterrizaje.");
            }

            // After any initial redirects, try to ensure we are on the correct page.
            if (!driver.getCurrentUrl().contains(sectionUrl)) {
                System.out.println("La URL actual no es la de la sección. Reintentando navegar a: " + sectionUrl);
                driver.get(sectionUrl);
            }
            
            // Finally, wait until the URL contains the desired section URL.
            wait.until(ExpectedConditions.urlContains(sectionUrl));
            Thread.sleep(3000); // Additional buffer for page load/dynamic content

            // 2. Find all potential thread links on the current page
            // Wait for the feed-list container to be visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feed-list")));

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
    private static String sanitizeFilename(String name) {
        // Replace invalid characters for filenames with an underscore
        String sanitized = name.replaceAll("[^a-zA-Z0-9-_.]", "_"); 
        // Trim to prevent leading/trailing underscores and limit length if necessary
        return sanitized.trim();
    }
}