package com.tearsdeepmind;

import com.tearsdeepmind.entity.DailyAnalysisEntity;
import com.tearsdeepmind.repository.DailyAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class HistoryIntegrationTest {

    @Autowired
    private DailyAnalysisRepository repository;

    @Test
    public void testSaveAndRetrieveJson() {
        String date = "2025-12-31";
        Map<String, Object> data = new HashMap<>();
        data.put("regime", "bullish");
        data.put("vix", 14.5);
        Map<String, Object> inner = new HashMap<>();
        inner.put("key_level", 4800);
        data.put("structure", inner);

        DailyAnalysisEntity entity = new DailyAnalysisEntity(date, data);
        repository.save(entity);

        Optional<DailyAnalysisEntity> retrieved = repository.findById(date);
        assertTrue(retrieved.isPresent());
        assertEquals("bullish", retrieved.get().getData().get("regime"));
        assertEquals(14.5, retrieved.get().getData().get("vix"));
        
        // Cleanup
        repository.deleteById(date);
    }
}
