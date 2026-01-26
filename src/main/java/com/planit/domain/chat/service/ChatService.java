package com.planit.domain.chat.service;

import com.planit.domain.chat.dto.AiChatRequest;
import com.planit.domain.chat.dto.AiChatResponse;
import com.planit.domain.chat.dto.ChatMessageRequest;
import com.planit.domain.chat.dto.ChatMessageResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final AiChatClient aiChatClient;

    public ChatService(AiChatClient aiChatClient) {
        this.aiChatClient = aiChatClient;
    }

    public ChatMessageResponse handleUserMessage(ChatMessageRequest request) {
        // 일정 조회/수정 로직은 추후 구현 예정이라고 가정하고, 현재는 AI 서버에 메시지만 전달
        AiChatResponse aiResponse = aiChatClient.requestChat(new AiChatRequest(
                request.tripId(),
                request.message()
        ));

        String message = aiResponse != null && aiResponse.message() != null
                ? aiResponse.message()
                : "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.";

        return new ChatMessageResponse(request.tripId(), "AI", message);
    }
}
