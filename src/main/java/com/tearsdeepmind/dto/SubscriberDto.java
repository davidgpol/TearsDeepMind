package com.tearsdeepmind.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubscriberDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    String name,
    
    boolean isActive
) {}
