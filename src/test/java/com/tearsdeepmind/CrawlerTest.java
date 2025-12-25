package com.tearsdeepmind;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class CrawlerTest {

    @Autowired
    private Crawler crawler;

    @MockBean
    private WebDriverFactory mockWebDriverFactory;

    @SpyBean // Use SpyBean to partially mock Crawler and call real methods for others
    private Crawler spyCrawler;

    @Test
    void contextLoads() {
        assertThat(crawler).isNotNull();
    }

    @Test
    void testStartCrawlingInitiatesLogin() throws InterruptedException {
        // Mock WebElement for email and password inputs
        WebElement mockEmailInput = mock(WebElement.class);
        WebElement mockPasswordInput = mock(WebElement.class);
        
        // Mock the createWebDriver method of the WebDriverFactory to return a mock WebDriver
        WebDriver mockWebDriver = mock(WebDriver.class);
        doReturn(mockWebDriver).when(mockWebDriverFactory).createWebDriver();

        // Mock WebDriver.Options and WebDriver.Window
        WebDriver.Options mockOptions = mock(WebDriver.Options.class);
        WebDriver.Window mockWindow = mock(WebDriver.Window.class);
        when(mockWebDriver.manage()).thenReturn(mockOptions);
        when(mockOptions.window()).thenReturn(mockWindow);

        // Configure mock WebElements for ExpectedConditions.elementToBeClickable
        when(mockEmailInput.isDisplayed()).thenReturn(true);
        when(mockEmailInput.isEnabled()).thenReturn(true);
        when(mockPasswordInput.isDisplayed()).thenReturn(true);
        when(mockPasswordInput.isEnabled()).thenReturn(true);

        // Configure mockWebDriver to return mock WebElements when findElement is called
        when(mockWebDriver.findElement(By.name("email"))).thenReturn(mockEmailInput);
        when(mockWebDriver.findElement(By.name("password"))).thenReturn(mockPasswordInput);

        // Mock the createOutputDirectories method to prevent actual directory creation during test
        doReturn(Paths.get("mockPath")).when(spyCrawler).createOutputDirectories();

        // Mock the crawlAndExtractData method to prevent actual crawling during test
        doNothing().when(spyCrawler).crawlAndExtractData(any(WebDriver.class), any(Path.class), any(WebDriverWait.class));

        // Call the initializeAndStart method
        spyCrawler.initializeAndStart();

        // Verify interactions related to login
        verify(mockWebDriver).manage();
        verify(mockOptions).window();
        verify(mockWindow).maximize();
        verify(mockWebDriver).get(anyString()); // Verify get is called with any string (login URL)
        verify(mockEmailInput).sendKeys(anyString()); // Verify email input
        verify(mockPasswordInput).sendKeys(anyString()); // Verify password input
        verify(mockPasswordInput).submit(); // Verify form submission
    }
}

