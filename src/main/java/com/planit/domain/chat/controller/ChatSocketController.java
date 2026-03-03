package com.planit.domain.chat.controller;

import com.planit.domain.chat.dto.ChatMessageResponse;
import com.planit.domain.chat.dto.ChatSendRequest;
import com.planit.domain.chat.service.ChatService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/trips/{tripId}/chat.send")
    public void sendChat(
            @DestinationVariable Long tripId,
            ChatSendRequest request,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalStateException("WebSocket principal is required");
        }
        ChatMessageResponse response = chatService.sendUserMessage(tripId, request.content(), principal.getName());
        messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/chat", response);
    }
}
