package com.planit.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageRequest(
        @NotNull
        Long tripId,
        @NotBlank
        String message
) {
}
