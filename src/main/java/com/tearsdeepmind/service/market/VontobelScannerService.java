package com.tearsdeepmind.service.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tearsdeepmind.domain.model.TurboProduct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class VontobelScannerService {
    private static final Logger logger = LoggerFactory.getLogger(VontobelScannerService.class);
    private static final String BASE_URL = "https://markets.vontobel.com/de-de/produkte/hebel/turbo-optionsscheine";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    private final ObjectMapper objectMapper;

    public VontobelScannerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TurboProduct> scan(String underlyingSymbol, String direction) {
        String underlyingId = mapUnderlying(underlyingSymbol);
        if (underlyingId == null) {
            logger.error("Underlying {} not mapped.", underlyingSymbol);
            return List.of();
        }

        String directionVal = "LONG".equalsIgnoreCase(direction) ? "1" : "2";

        try {
            logger.info("Scanning Vontobel (TR Only) for {} {}...", underlyingSymbol, direction);
            
            Document doc = Jsoup.connect(BASE_URL)
                    .data("underlying", underlyingId)
                    .data("direction", directionVal)
                    .data("platforms", "1") // 1 = Trade Republic / LS Exchange
                    .header("Accept-Language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7")
                    .userAgent(USER_AGENT)
                    .timeout(30000) // Increased timeout to 30 seconds for stability
                    .get();

            Element script = doc.selectFirst("script#__NEXT_DATA__");
            if (script == null) {
                logger.error("Vontobel parsing failed: __NEXT_DATA__ missing.");
                return List.of();
            }

            JsonNode root = objectMapper.readTree(script.html());
            
            // Debug Path Traversal
            JsonNode pageProps = root.path("props").path("pageProps");
            if (pageProps.isMissingNode()) {
                logger.error("Vontobel Debug: 'pageProps' missing. Available keys in root: {}", getKeys(root));
                return List.of();
            }
            
            JsonNode additionalData = pageProps.path("additionalData");
            if (additionalData.isMissingNode()) {
                logger.error("Vontobel Debug: 'additionalData' missing. Available keys in pageProps: {}", getKeys(pageProps));
                return List.of();
            }
            
            JsonNode productSearchData = additionalData.path("productSearchData");
            if (productSearchData.isMissingNode()) {
                logger.error("Vontobel Debug: 'productSearchData' missing. Available keys in additionalData: {}", getKeys(additionalData));
                return List.of();
            }

            JsonNode items = productSearchData.path("items");

            if (items.isMissingNode() || !items.isArray()) {
                logger.warn("Vontobel Debug: 'items' missing or not array. Available keys in productSearchData: {}", getKeys(productSearchData));
                return List.of();
            }
            
            logger.info("Vontobel Debug: Found {} items in JSON.", items.size());

            List<TurboProduct> products = new ArrayList<>();
            for (JsonNode item : items) {
                // STRICT FILTER (Python Logic Replication): 
                // Only discard if the field explicitly exists and is false.
                // If the field is missing, we trust the URL 'platforms=1' filter.
                if (item.has("isTradeRepublic") && !item.path("isTradeRepublic").asBoolean()) {
                    continue;
                }

                // Logic aligned with Python script
                String isin = item.path("isin").asText(null);
                if (isin == null) continue;

                double leverageVal = item.path("leverage").asDouble(0.0);
                if (leverageVal <= 0) continue;

                BigDecimal leverage = BigDecimal.valueOf(leverageVal);
                
                // Price path logic
                JsonNode priceNode = item.path("price");
                BigDecimal bid = BigDecimal.valueOf(priceNode.path("bid").asDouble(0.0));
                BigDecimal ask = BigDecimal.valueOf(priceNode.path("ask").asDouble(0.0));
                BigDecimal ratio = BigDecimal.valueOf(item.path("ratio").asDouble(0.0));
                
                // Barrier/Strike logic
                BigDecimal knockOut = BigDecimal.valueOf(item.path("knockOut").asDouble(0.0));
                BigDecimal strike = BigDecimal.valueOf(item.path("strike").asDouble(0.0));
                
                // Fallback logic if strike is missing but KO exists (common in JSON)
                if (strike.compareTo(BigDecimal.ZERO) == 0 && knockOut.compareTo(BigDecimal.ZERO) > 0) {
                    strike = knockOut;
                }
                
                // Use barrierDistance if needed for sorting, but we rely on leverage
                TurboProduct product = new TurboProduct(isin, direction.toUpperCase(), strike, knockOut, leverage, bid, ask, ratio);
                products.add(product);
            }

            // Sort by leverage descending (most aggressive first)
            products.sort(Comparator.comparing(TurboProduct::leverage).reversed());
            
            logger.info("Found {} turbo products for {} {}", products.size(), underlyingSymbol, direction);
            return products;

        } catch (IOException e) {
            logger.error("Error scanning Vontobel: {}", e.getMessage());
            return List.of();
        }
    }

    private TurboProduct mapToProduct(JsonNode item, String direction) {
        try {
            String isin = item.path("isin").asText(null);
            if (isin == null) return null;

            double leverageVal = item.path("leverage").asDouble(0.0);
            if (leverageVal <= 0) return null;

            BigDecimal leverage = BigDecimal.valueOf(leverageVal);
            BigDecimal bid = BigDecimal.valueOf(item.path("price").path("bid").asDouble(0.0));
            BigDecimal ask = BigDecimal.valueOf(item.path("price").path("ask").asDouble(0.0));
            BigDecimal ratio = BigDecimal.valueOf(item.path("ratio").asDouble(0.0));
            
            BigDecimal knockOut = BigDecimal.valueOf(item.path("knockOut").asDouble(0.0));
            BigDecimal strike = BigDecimal.valueOf(item.path("strike").asDouble(0.0));
            
            // Fallback logic from Python script: If strike missing, use KO
            if (strike.compareTo(BigDecimal.ZERO) == 0 && knockOut.compareTo(BigDecimal.ZERO) > 0) {
                strike = knockOut;
            }

            return new TurboProduct(isin, direction.toUpperCase(), strike, knockOut, leverage, bid, ask, ratio);

        } catch (Exception e) {
            return null;
        }
    }

    private String mapUnderlying(String symbol) {
        if (symbol == null) return null;
        return switch (symbol.toUpperCase()) {
            case "SPX", "^GSPC" -> "70";
            case "DAX", "^GDAXI" -> "1";
            case "NDX", "^NDX" -> "72";
            default -> null;
        };
    }
    
    private List<String> getKeys(JsonNode node) {
        List<String> keys = new ArrayList<>();
        if (node != null && node.isObject()) {
            node.fieldNames().forEachRemaining(keys::add);
        }
        return keys;
    }
}
