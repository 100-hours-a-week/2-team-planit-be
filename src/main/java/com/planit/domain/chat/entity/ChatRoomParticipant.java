package com.planit.domain.chat.entity;

import com.planit.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "chat_room_participant",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_room_participant", columnNames = {"chat_room_id", "user_id"})
)
public class ChatRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_count", nullable = false)
    private Long lastReadCount;

    protected ChatRoomParticipant() {
    }

    public ChatRoomParticipant(ChatRoom chatRoom, User user) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.lastReadCount = 0L;
    }

    public Long getId() {
        return id;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public User getUser() {
        return user;
    }

    public Long getLastReadCount() {
        return lastReadCount;
    }

    public void markRead(Long totalMessageCount) {
        this.lastReadCount = totalMessageCount == null ? 0L : totalMessageCount;
    }
}
