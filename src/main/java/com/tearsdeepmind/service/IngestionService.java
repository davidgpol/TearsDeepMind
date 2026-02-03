package com.tearsdeepmind.service;

import com.tearsdeepmind.entity.RawDocumentEntity;
import com.tearsdeepmind.repository.RawDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);
    private final RawDocumentRepository repository;

    public IngestionService(RawDocumentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UUID saveRawDocument(LocalDate date, String type, String content) {
        String checksum = calculateChecksum(content);
        
        Optional<RawDocumentEntity> existing = repository.findByDateAndType(date, type);
        if (existing.isPresent()) {
            if (existing.get().getChecksum().equals(checksum)) {
                logger.info("Document already exists and checksum matches for {} - {}. Skipping.", date, type);
                return existing.get().getId();
            }
            logger.info("Document exists but checksum differs for {} - {}. Updating.", date, type);
            // In a real lineage system, we might want to version this. For now, we update.
            repository.delete(existing.get());
        }

        RawDocumentEntity entity = new RawDocumentEntity(date, type, content, checksum);
        RawDocumentEntity saved = repository.save(entity);
        logger.info("Saved raw document ID: {} for {} - {}", saved.getId(), date, type);
        return saved.getId();
    }

    public Optional<RawDocumentEntity> getRawDocument(LocalDate date, String type) {
        return repository.findByDateAndType(date, type);
    }

    private String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
