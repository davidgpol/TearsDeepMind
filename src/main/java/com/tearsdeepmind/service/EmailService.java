package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.DailyAnalysisDto;
import com.tearsdeepmind.entity.SubscriberEntity;
import com.tearsdeepmind.repository.SubscriberRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SubscriberRepository subscriberRepository;

    @Value("${tears.notifications.from}")
    private String from;

    public EmailService(JavaMailSender mailSender, SubscriberRepository subscriberRepository) {
        this.mailSender = mailSender;
        this.subscriberRepository = subscriberRepository;
    }

    public void sendReportWithAttachment(File reportFile, DailyAnalysisDto analysisDto) {
        try {
            List<String> activeRecipients = subscriberRepository.findByIsActiveTrue()
                    .stream()
                    .map(SubscriberEntity::getEmail)
                    .toList();

            if (activeRecipients.isEmpty()) {
                logger.warn("No active subscribers found in database. Skipping email notification.");
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(activeRecipients.toArray(new String[0]));
            helper.setFrom(from);

            String bias = extractField(analysisDto, "Bias", "N/A");
            String regime = extractField(analysisDto, "Regime", "N/A");
            String date = analysisDto.date();

            helper.setSubject(String.format("[%s] %s | %s", bias, regime, date));

            String thesis = extractField(analysisDto, "Thesis", "No summary available.");
            helper.setText(thesis);

            FileSystemResource file = new FileSystemResource(reportFile);
            helper.addAttachment(reportFile.getName(), file);

            mailSender.send(message);
            logger.info("Daily report email sent successfully to {} subscribers: {}", activeRecipients.size(), activeRecipients);

        } catch (MessagingException e) {
            logger.error("Failed to construct or send email notification", e);
        } catch (Exception e) {
            logger.error("Unexpected error during email notification process", e);
        }
    }

    private String extractField(DailyAnalysisDto dto, String key, String defaultValue) {
        if (dto.data() != null && dto.data().containsKey(key)) {
            Object value = dto.data().get(key);
            return value != null ? value.toString() : defaultValue;
        }
        return defaultValue;
    }
}
