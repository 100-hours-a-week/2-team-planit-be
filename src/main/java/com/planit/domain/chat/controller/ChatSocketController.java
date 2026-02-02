package com.planit.domain.chat.controller;

import com.planit.domain.chat.dto.ChatMessageRequest;
import com.planit.domain.chat.dto.ChatMessageResponse;
import com.planit.domain.chat.service.ChatService;
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

    @MessageMapping("/trips/{tripId}/chat")
    public void handleChat(@DestinationVariable Long tripId, ChatMessageRequest request) {
        // 클라이언트가 보낸 메시지를 먼저 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/trips/" + tripId + "/chat",
                new ChatMessageResponse(tripId, "USER", request.message())
        );

        // AI 응답을 생성하고 브로드캐스트
        ChatMessageResponse response = chatService.handleUserMessage(
                new ChatMessageRequest(tripId, request.message())
        );
        messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/chat", response);
    }
}
