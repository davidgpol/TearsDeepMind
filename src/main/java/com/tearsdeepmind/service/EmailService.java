package com.tearsdeepmind.service;

import com.tearsdeepmind.domain.model.MarketMemoryRecord;
import com.tearsdeepmind.entity.SubscriberEntity;
import com.tearsdeepmind.repository.SubscriberRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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

    public void sendReport(LocalDate date, String markdownContent, MarketMemoryRecord marketData) {
        try {
            List<String> activeRecipients = subscriberRepository.findByIsActiveTrue()
                    .stream()
                    .map(SubscriberEntity::getEmail)
                    .toList();

            if (activeRecipients.isEmpty()) {
                logger.warn("No active subscribers found. Skipping email.");
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // true = multipart

            helper.setTo(activeRecipients.toArray(new String[0]));
            helper.setFrom(from);

            String bias = (marketData != null) ? marketData.sentiment_profile().bias() : "N/A";
            String headline = (marketData != null) ? marketData.daily_thesis().headline() : "Daily Report";
            
            helper.setSubject(String.format("[%s] %s | %s", bias.toUpperCase(), date, headline));
            
            // Body text
            helper.setText("Adjunto encontrarás el informe detallado para la fecha " + date + ".\n\nBias: " + bias + "\nHeadline: " + headline);
            
            // Attachment
            org.springframework.core.io.ByteArrayResource reportResource = new org.springframework.core.io.ByteArrayResource(markdownContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            helper.addAttachment("TearsReport_" + date + ".md", reportResource);

            mailSender.send(message);
            logger.info("Report sent to {} subscribers.", activeRecipients.size());

        } catch (MessagingException e) {
            logger.error("Failed to send email", e);
        }
    }
}