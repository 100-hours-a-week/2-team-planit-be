package com.planit.domain.chat.service;

import com.planit.domain.chat.document.ChatMessageDocument;
import com.planit.domain.chat.dto.ChatMessageResponse;
import com.planit.domain.chat.repository.ChatMessageMongoRepository;
import com.planit.domain.trip.config.RedisStreamProperties;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ChatResultListener implements StreamListener<String, MapRecord<String, String, String>> {
    private static final Logger log = LoggerFactory.getLogger(ChatResultListener.class);
    private static final String CONSUMER_GROUP = "chat-workers";

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final RedisStreamProperties streamProperties;
    private final ChatMessageMongoRepository messageRepository;
    private final S3ImageUrlResolver imageUrlResolver;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> fields = message.getValue();
        String chatMessageId = fields.get("chatMessageId");
        if (!StringUtils.hasText(chatMessageId)) {
            log.warn("Invalid chat result message: {}", fields);
            ack(message);
            return;
        }
        ChatMessageResponse response = buildChatMessageResponse(chatMessageId);
        if (response == null) {
            log.warn("AI chat message not found for id={}", chatMessageId);
            ack(message);
            return;
        }
        messagingTemplate.convertAndSend("/topic/chat/" + response.tripId(), response);
        ack(message);
    }

    private void ack(MapRecord<String, String, String> message) {
        try {
            redisTemplate.opsForStream().acknowledge(streamProperties.getChatResultsKey(), CONSUMER_GROUP, message.getId());
            log.info("[CHAT] acked id={}", message.getId());
        } catch (Exception ex) {
            log.warn("Failed to ack chat result message: {}", message.getId(), ex);
        }
    }

    private ChatMessageResponse buildChatMessageResponse(String chatMessageId) {
        Optional<ChatMessageDocument> aiMessage = messageRepository.findById(chatMessageId);
        if (aiMessage.isEmpty()) {
            return null;
        }
        ChatMessageDocument document = aiMessage.get();
        String profileImageUrl;
        if ("AI".equalsIgnoreCase(document.getSenderType())) {
            profileImageUrl = "https://dgs44b7nvvjo3.cloudfront.net/ai.png";
        } else if (StringUtils.hasText(document.getSenderProfileImageKey())) {
            profileImageUrl = imageUrlResolver.resolve(document.getSenderProfileImageKey());
        } else {
            profileImageUrl = null;
        }
        return new ChatMessageResponse(
                document.getId(),
                document.getTripId(),
                document.getSenderUserId(),
                document.getSenderNickname(),
                profileImageUrl,
                document.getSenderType(),
                document.getContent(),
                document.getCreatedAt(),
                document.getSeq()
        );
    }
}
