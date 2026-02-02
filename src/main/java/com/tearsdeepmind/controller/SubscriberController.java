package com.tearsdeepmind.controller;

import com.tearsdeepmind.dto.SubscriberDto;
import com.tearsdeepmind.service.SubscriberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscribers")
@Tag(name = "Subscriber Operations", description = "Endpoints for managing report email subscribers.")
public class SubscriberController {

    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @Operation(summary = "List all subscribers", description = "Retrieves a list of all registered report recipients.")
    @GetMapping
    public List<SubscriberDto> getAll() {
        return subscriberService.getAllSubscribers();
    }

    @Operation(summary = "Get subscriber details", description = "Retrieves details for a specific email.")
    @GetMapping("/{email}")
    public ResponseEntity<SubscriberDto> getOne(@PathVariable String email) {
        return subscriberService.getSubscriber(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create new subscriber", description = "Adds a new email to the notification list.")
    @PostMapping
    public ResponseEntity<SubscriberDto> create(@Valid @RequestBody SubscriberDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriberService.createSubscriber(dto));
    }

    @Operation(summary = "Toggle subscriber status", description = "Activates or deactivates a subscriber.")
    @PatchMapping("/{email}/status")
    public ResponseEntity<SubscriberDto> updateStatus(@PathVariable String email, @RequestBody Map<String, Boolean> status) {
        Boolean isActive = status.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest().build();
        }
        return subscriberService.updateStatus(email, isActive)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete subscriber", description = "Permanently removes an email from the list.")
    @DeleteMapping("/{email}")
    public ResponseEntity<Void> delete(@PathVariable String email) {
        subscriberService.deleteSubscriber(email);
        return ResponseEntity.noContent().build();
    }
}
