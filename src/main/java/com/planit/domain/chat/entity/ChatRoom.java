package com.planit.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_room")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, unique = true)
    private Long tripId;

    @Column(name = "total_message_count", nullable = false)
    private Long totalMessageCount;

    protected ChatRoom() {
    }

    public ChatRoom(Long tripId) {
        this.tripId = tripId;
        this.totalMessageCount = 0L;
    }

    public Long getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getTotalMessageCount() {
        return totalMessageCount;
    }

    public long incrementAndGet() {
        this.totalMessageCount = this.totalMessageCount + 1;
        return this.totalMessageCount;
    }
}
