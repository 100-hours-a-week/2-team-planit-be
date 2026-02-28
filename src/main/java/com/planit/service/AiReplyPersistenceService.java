package com.planit.service;

import com.planit.domain.chat.document.ChatMessageDocument;
import com.planit.domain.chat.entity.ChatRoom;
import com.planit.domain.chat.repository.ChatMessageMongoRepository;
import com.planit.domain.chat.repository.ChatRoomRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiReplyPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(AiReplyPersistenceService.class);
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageMongoRepository messageRepository;

    @Transactional
    public ChatMessageDocument saveAiReply(String tripId, String content) {
        long tripIdValue = parseTripId(tripId);
        ChatRoom chatRoom = chatRoomRepository.findByTripIdForUpdate(tripIdValue)
                .orElseGet(() -> chatRoomRepository.save(new ChatRoom(tripIdValue)));
        long seq = chatRoom.incrementAndGet();
        chatRoomRepository.save(chatRoom);

        ChatMessageDocument aiMessage = new ChatMessageDocument(
                tripIdValue,
                null,
                "AI Planner",
                "ai.png",
                "AI",
                content,
                Instant.now(),
                seq
        );
        ChatMessageDocument saved = messageRepository.save(aiMessage);
        log.info("Persisted AI reply tripId={} seq={}", tripIdValue, seq);
        return saved;
    }

    private long parseTripId(String tripId) {
        try {
            return Long.parseLong(tripId);
        } catch (NumberFormatException ex) {
            log.warn("Invalid tripId format for AI reply tripId={}", tripId, ex);
            throw new IllegalArgumentException("Invalid tripId: " + tripId, ex);
        }
    }
}
