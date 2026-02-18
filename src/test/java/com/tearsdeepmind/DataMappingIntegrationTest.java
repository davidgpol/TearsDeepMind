package com.tearsdeepmind;

import com.tearsdeepmind.domain.model.QuantMemoryRecord;
import com.tearsdeepmind.entity.QuantMemoryEntity;
import com.tearsdeepmind.repository.QuantMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DataMappingIntegrationTest {

    @Autowired
    private QuantMemoryRepository repository;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    public void testJsonbMappingWithStrongTypes() {
        LocalDate date = LocalDate.of(2026, 2, 2);
        
        QuantMemoryRecord.ExtractedLevel level = new QuantMemoryRecord.ExtractedLevel(
            "7000 pivot", List.of(7000.0), "pivot", "high", "80%"
        );
        
        QuantMemoryRecord.DefinedZones zones = new QuantMemoryRecord.DefinedZones(
            List.of("6900-6920"), List.of("7100"), List.of(), List.of()
        );
        
        QuantMemoryRecord record = new QuantMemoryRecord(date, List.of(level), zones, "Test commentary");
        
        QuantMemoryEntity entity = new QuantMemoryEntity(date, record, null, "v1", "test-model");
        repository.save(entity);

        QuantMemoryEntity retrieved = repository.findById(date).orElseThrow();
        assertEquals("Test commentary", retrieved.getData().quant_commentary());
        assertEquals(7000.0, retrieved.getData().extracted_levels().get(0).numeric_values().get(0));
        assertEquals("6900-6920", retrieved.getData().defined_zones().support().get(0));
    }
}
