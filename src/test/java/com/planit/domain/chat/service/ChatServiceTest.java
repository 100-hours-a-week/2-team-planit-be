package com.planit.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.planit.domain.chat.document.ChatMessageDocument;
import com.planit.domain.chat.dto.ChatMessageResponse;
import com.planit.domain.chat.dto.ChatSummaryResponse;
import com.planit.domain.chat.entity.ChatRoom;
import com.planit.domain.chat.entity.ChatRoomParticipant;
import com.planit.domain.chat.repository.ChatMessageMongoRepository;
import com.planit.domain.chat.repository.ChatRoomParticipantRepository;
import com.planit.domain.chat.repository.ChatRoomRepository;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.service.TripAccessService;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.ai.client.AiApiClient;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatRoomParticipantRepository participantRepository;
    @Mock
    private ChatMessageMongoRepository chatMessageMongoRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripAccessService tripAccessService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3ImageUrlResolver imageUrlResolver;
    @Mock
    private AiApiClient aiApiClient;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chatService = new ChatService(
                chatRoomRepository,
                participantRepository,
                chatMessageMongoRepository,
                tripRepository,
                tripAccessService,
                userRepository,
                imageUrlResolver,
                aiApiClient,
                messagingTemplate
        );
    }

    @Test
    void sendUserMessage_incrementsCountAndMarksRead() {
        User user = User.builder()
                .id(10L)
                .loginId("user1")
                .password("hashed")
                .nickname("nick")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Trip trip = new Trip(
                user,
                "title",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "Seoul",
                10000
        );
        ReflectionTestUtils.setField(trip, "id", 1L);

        ChatRoom room = new ChatRoom(1L);
        ReflectionTestUtils.setField(room, "id", 100L);
        ChatRoomParticipant participant = new ChatRoomParticipant(room, user);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripAccessService.requireReadable(trip, "user1"))
                .thenReturn(new TripAccessService.AccessInfo(user, true, true));
        when(chatRoomRepository.findByTripIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByChatRoomIdAndUserId(100L, 10L)).thenReturn(Optional.of(participant));
        when(chatMessageMongoRepository.save(any(ChatMessageDocument.class))).thenAnswer(invocation -> {
            ChatMessageDocument doc = invocation.getArgument(0);
            ReflectionTestUtils.setField(doc, "id", "mongo-id-1");
            return doc;
        });
        when(imageUrlResolver.resolve(null)).thenReturn("https://default-profile.png");

        ChatMessageResponse response = chatService.sendUserMessage(1L, "hello", "user1", "jwt-token");

        assertThat(response.seq()).isEqualTo(1L);
        assertThat(response.senderUserId()).isEqualTo(10L);
        assertThat(response.senderNickname()).isEqualTo("nick");
        assertThat(response.senderProfileImageUrl()).isEqualTo("https://default-profile.png");
        assertThat(participant.getLastReadCount()).isEqualTo(1L);
    }

    @Test
    void summary_calculatesUnreadCount() {
        User user = User.builder()
                .id(10L)
                .loginId("user1")
                .password("hashed")
                .nickname("nick")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Trip trip = new Trip(
                user,
                "title",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "Seoul",
                10000
        );
        ReflectionTestUtils.setField(trip, "id", 1L);

        ChatRoom room = new ChatRoom(1L);
        ReflectionTestUtils.setField(room, "id", 100L);
        ReflectionTestUtils.setField(room, "totalMessageCount", 8L);

        ChatRoomParticipant participant = new ChatRoomParticipant(room, user);
        participant.markRead(3L);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripAccessService.requireReadable(trip, "user1"))
                .thenReturn(new TripAccessService.AccessInfo(user, true, true));
        when(chatRoomRepository.findByTripId(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByChatRoomIdAndUserId(100L, 10L)).thenReturn(Optional.of(participant));
        when(chatMessageMongoRepository.findByTripIdOrderByCreatedAtDesc(any(), any())).thenReturn(List.of(
                new ChatMessageDocument(
                        1L,
                        10L,
                        null,
                        null,
                        "USER",
                        "hello",
                        Instant.now(),
                        8L
                )
        ));
        when(userRepository.findAllById(any())).thenReturn(List.of(user));
        when(imageUrlResolver.resolve(null)).thenReturn("https://default-profile.png");

        ChatSummaryResponse response = chatService.getSummary(1L, "user1");
        ChatMessageResponse messageResponse = chatService.getMessages(1L, "user1", 20, null).get(0);

        assertThat(response.totalMessageCount()).isEqualTo(8L);
        assertThat(response.unreadCount()).isEqualTo(5L);
        assertThat(messageResponse.senderNickname()).isEqualTo("nick");
        assertThat(messageResponse.senderProfileImageUrl()).isEqualTo("https://default-profile.png");
    }
}
