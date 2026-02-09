package com.tearsdeepmind;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class MarketDataSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testMarketDataSchemaExists() {
        // Verify schema exists
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'MARKET_DATA'", Integer.class);
        
        // H2 usually capitalizes schema names
        if (count == 0) {
             count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'market_data'", Integer.class);
        }
        
        assertTrue(count > 0, "Schema 'market_data' should exist");
    }

    @Test
    public void testMarketDataTablesExist() {
        // Verify tables exist in market_data schema
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'MARKET_DATA' OR table_schema = 'market_data'", 
            String.class);

        assertTrue(tables.contains("DAILY_CANDLES") || tables.contains("daily_candles"), "Table 'daily_candles' should exist");
        assertTrue(tables.contains("INTRADAY_CANDLES") || tables.contains("intraday_candles"), "Table 'intraday_candles' should exist");
        assertTrue(tables.contains("TECHNICAL_INDICATORS") || tables.contains("technical_indicators"), "Table 'technical_indicators' should exist");
        assertTrue(tables.contains("AUDIT_LOGS") || tables.contains("audit_logs"), "Table 'audit_logs' should exist");
    }

    @Test
    public void testTableColumns() {
        // Verify columns in daily_candles
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'DAILY_CANDLES' OR table_name = 'daily_candles'");
        
        boolean hasSymbol = columns.stream().anyMatch(m -> "SYMBOL".equalsIgnoreCase((String) m.get("COLUMN_NAME")));
        boolean hasDate = columns.stream().anyMatch(m -> "DATE".equalsIgnoreCase((String) m.get("COLUMN_NAME")));
        boolean hasClose = columns.stream().anyMatch(m -> "CLOSE".equalsIgnoreCase((String) m.get("COLUMN_NAME")));
        
        assertTrue(hasSymbol, "Column 'symbol' should exist in daily_candles");
        assertTrue(hasDate, "Column 'date' should exist in daily_candles");
        assertTrue(hasClose, "Column 'close' should exist in daily_candles");
    }
}
