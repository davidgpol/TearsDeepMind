package com.tearsdeepmind.service;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CrawlerServiceTest {

    @Test
    public void testParseDate() throws Exception {
        CrawlerService service = new CrawlerService();
        Method method = CrawlerService.class.getDeclaredMethod("parseDate", String.class);
        method.setAccessible(true);

        // Test cases
        assertEquals(LocalDate.of(2025, 12, 1), method.invoke(service, "Dic. 1, 2025"));
        assertEquals(LocalDate.of(2025, 1, 15), method.invoke(service, "Ene. 15, 2025"));
        assertEquals(LocalDate.of(2024, 7, 4), method.invoke(service, "Jul. 4, 2024"));
        assertEquals(LocalDate.now(), method.invoke(service, "Invalid Date")); // Should fallback to now
    }
}
