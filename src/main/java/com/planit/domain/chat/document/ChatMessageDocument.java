package com.planit.domain.chat.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_messages")
public class ChatMessageDocument {

    @Id
    private String id;

    private Long tripId;
    private Long senderUserId;
    private String senderNickname;
    private String senderProfileImageKey;
    private String senderType;
    private String content;
    private Instant createdAt;
    private Long seq;

    protected ChatMessageDocument() {
    }

    public ChatMessageDocument(
            Long tripId,
            Long senderUserId,
            String senderNickname,
            String senderProfileImageKey,
            String senderType,
            String content,
            Instant createdAt,
            Long seq
    ) {
        this.tripId = tripId;
        this.senderUserId = senderUserId;
        this.senderNickname = senderNickname;
        this.senderProfileImageKey = senderProfileImageKey;
        this.senderType = senderType;
        this.content = content;
        this.createdAt = createdAt;
        this.seq = seq;
    }

    public String getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public String getSenderProfileImageKey() {
        return senderProfileImageKey;
    }

    public String getSenderType() {
        return senderType;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getSeq() {
        return seq;
    }
}
