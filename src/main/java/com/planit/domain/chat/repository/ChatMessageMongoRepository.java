package com.planit.domain.chat.repository;

import com.planit.domain.chat.document.ChatMessageDocument;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {

    List<ChatMessageDocument> findByTripIdOrderByCreatedAtDesc(Long tripId, PageRequest pageable);

    List<ChatMessageDocument> findByTripIdAndCreatedAtBeforeOrderByCreatedAtDesc(Long tripId, Instant before, PageRequest pageable);
}
