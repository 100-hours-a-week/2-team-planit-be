package com.planit.domain.chat.controller;

import com.planit.domain.chat.dto.ChatMessageResponse;
import com.planit.domain.chat.dto.ChatReadResponse;
import com.planit.domain.chat.dto.ChatSummaryResponse;
import com.planit.domain.chat.service.ChatService;
import com.planit.global.common.exception.UnauthorizedAccessException;
import com.planit.global.common.response.ApiResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripChatController {

    private final ChatService chatService;

    public TripChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/trips/{tripId}/chat/summary")
    public ResponseEntity<ApiResponse<ChatSummaryResponse>> getSummary(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        ChatSummaryResponse response = chatService.getSummary(tripId, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/trips/{tripId}/chat/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }

        Instant beforeInstant = null;
        if (before != null && !before.isBlank()) {
            try {
                beforeInstant = Instant.parse(before);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid before parameter");
            }
        }

        List<ChatMessageResponse> response = chatService.getMessages(tripId, principal.getUsername(), limit, beforeInstant);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/trips/{tripId}/chat/read")
    public ResponseEntity<ApiResponse<ChatReadResponse>> markRead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        ChatReadResponse response = chatService.readAll(tripId, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
