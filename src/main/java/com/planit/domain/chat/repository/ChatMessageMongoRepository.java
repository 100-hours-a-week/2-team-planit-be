package com.planit.domain.chat.repository;

import com.planit.domain.chat.document.ChatMessageDocument;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {
    List<ChatMessageDocument> findByTripIdOrderByCreatedAtDesc(Long tripId, Pageable pageable);

    List<ChatMessageDocument> findByTripIdAndCreatedAtBeforeOrderByCreatedAtDesc(Long tripId, Instant before, Pageable pageable);
}
