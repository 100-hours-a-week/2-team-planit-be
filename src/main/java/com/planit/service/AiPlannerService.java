package com.planit.service;

import com.planit.ai.client.AiApiClient;
import com.planit.ai.dto.AiRequest;
import com.planit.domain.chat.document.ChatMessageDocument;
import com.planit.domain.chat.repository.ChatMessageMongoRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiPlannerService {
    private static final Logger log = LoggerFactory.getLogger(AiPlannerService.class);
    private final ChatMessageMongoRepository messageRepository;
    private final RedisLockService lockService;
    private final AiApiClient aiApiClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final AiReplyPersistenceService replyPersistenceService;
    @Value("${app.redis.stream.chatResultsKey:stream:chat-results}")
    private String chatResultsStreamKey;

    public void process(String recordId, String messageId, String tripId) {
        ChatMessageDocument message = messageRepository.findById(messageId).orElse(null);
        handleMessage(recordId, message, tripId);
    }

    private void handleMessage(String recordId, ChatMessageDocument message, String tripId) {
        if (message == null) {
            log.warn("chat message not found for record={} trip={}", recordId, tripId);
            ack(recordId);
            return;
        }
        String content = message.getContent();
        if (content == null || !content.contains("@AI")) {
            ack(recordId);
            return;
        }
        String lockKey = "ai:lock:trip:" + tripId;
        boolean locked = false;
        try {
            locked = lockService.tryLock(lockKey, Duration.ofSeconds(20));
            if (!locked) {
                publishFallback(tripId, message.getId(), "AI is currently processing another request. Please try again.");
                return;
            }
            String cleaned = content.replace("@AI", "").trim();
            String reply = aiApiClient.requestAiReply(new AiRequest(tripId, cleaned));
            ChatMessageDocument savedMessage = replyPersistenceService.saveAiReply(tripId, reply);
            publishReply(tripId, message.getId(), reply, savedMessage.getId());
        } catch (Exception ex) {
            log.error("AI call failed trip={} message={}", tripId, message.getId(), ex);
            publishFallback(tripId, message.getId(), "AI request failed. Please try again.");
        } finally {
            if (locked) {
                lockService.release(lockKey);
            }
            ack(recordId);
        }
    }

    private void publishReply(String tripId, String messageId, String content, String chatMessageId) {
        Map<String, String> fields = new HashMap<>();
        fields.put("tripId", tripId);
        fields.put("replyToMessageId", messageId);
        fields.put("content", content);
        fields.put("senderType", "AI");
        fields.put("chatMessageId", chatMessageId);
        redisTemplate.opsForStream().add(chatResultsStreamKey, fields);
    }

    private void publishFallback(String tripId, String messageId, String content) {
        Map<String, String> fields = new HashMap<>();
        fields.put("tripId", tripId);
        fields.put("replyToMessageId", messageId);
        fields.put("content", content);
        fields.put("senderType", "AI");
        redisTemplate.opsForStream().add(chatResultsStreamKey, fields);
    }

    private void ack(String recordId) {
        redisTemplate.opsForStream().acknowledge("stream:ai-jobs", "ai-workers", recordId);
    }

}
