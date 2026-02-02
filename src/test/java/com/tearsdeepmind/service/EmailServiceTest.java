package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.DailyAnalysisDto;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(emailService, "recipients", "test1@test.com,test2@test.com");
        ReflectionTestUtils.setField(emailService, "from", "bot@tears.com");
        
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void sendReportWithAttachment_Success() throws Exception {
        // Arrange
        File tempFile = File.createTempFile("test_report", ".md");
        tempFile.deleteOnExit();
        
        DailyAnalysisDto dto = new DailyAnalysisDto("2026-01-30", Map.of(
                "Bias", "BULLISH",
                "Regime", "Trending",
                "Thesis", "Market is strong."
        ));

        // Act
        emailService.sendReportWithAttachment(tempFile, dto);

        // Assert
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());
        
        MimeMessage sentMessage = messageCaptor.getValue();
        // Simple assertions on subject (as body/recipients are harder to extract from raw MimeMessage without more boilerplate)
        assertEquals("[BULLISH] Trending | 2026-01-30", sentMessage.getSubject());
    }

    @Test
    void sendReportWithAttachment_HandlesMissingFields() throws Exception {
        // Arrange
        File tempFile = File.createTempFile("test_report_empty", ".md");
        tempFile.deleteOnExit();
        
        DailyAnalysisDto dto = new DailyAnalysisDto("2026-01-30", Map.of()); // Empty data map

        // Act
        emailService.sendReportWithAttachment(tempFile, dto);

        // Assert
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());
        
        MimeMessage sentMessage = messageCaptor.getValue();
        assertEquals("[N/A] N/A | 2026-01-30", sentMessage.getSubject());
    }
}
