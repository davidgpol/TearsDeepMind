package com.tearsdeepmind.controller;

import com.tearsdeepmind.entity.RawDocumentEntity;
import com.tearsdeepmind.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/ingestion")
@Tag(name = "Ingestion Operations", description = "Endpoints for managing raw market data documents.")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Operation(summary = "Get raw document", description = "Retrieves the original text content by date and type (QUANT/MACRO).")
    @GetMapping("/raw/{date}/{type}")
    public ResponseEntity<String> getRaw(@PathVariable String date, @PathVariable String type) {
        return ingestionService.getRawDocument(LocalDate.parse(date), type.toUpperCase())
                .map(RawDocumentEntity::getContent)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Manual upload", description = "Uploads or updates a raw document manually.")
    @PostMapping("/upload")
    public UUID upload(@RequestParam String date, @RequestParam String type, @RequestBody String content) {
        return ingestionService.saveRawDocument(LocalDate.parse(date), type.toUpperCase(), content);
    }
}
