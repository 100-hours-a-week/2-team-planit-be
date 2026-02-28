package com.planit.domain.chat.service;

import com.planit.domain.chat.document.ChatMessageDocument;
import com.planit.domain.chat.dto.ChatMessageResponse;
import com.planit.domain.chat.dto.ChatReadResponse;
import com.planit.domain.chat.dto.ChatSummaryResponse;
import com.planit.domain.chat.entity.ChatRoom;
import com.planit.domain.chat.entity.ChatRoomParticipant;
import com.planit.domain.chat.entity.ChatSenderType;
import com.planit.domain.chat.repository.ChatMessageMongoRepository;
import com.planit.domain.chat.repository.ChatRoomParticipantRepository;
import com.planit.domain.chat.repository.ChatRoomRepository;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.service.TripAccessService;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_LIMIT = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository participantRepository;
    private final ChatMessageMongoRepository chatMessageMongoRepository;
    private final TripRepository tripRepository;
    private final TripAccessService tripAccessService;
    private final UserRepository userRepository;
    private final S3ImageUrlResolver imageUrlResolver;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public ChatMessageResponse sendUserMessage(Long tripId, String content, String loginId) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        ChatContext context = getContext(tripId, loginId, true);
        long seq = context.chatRoom().incrementAndGet();

        ChatMessageDocument saved = chatMessageMongoRepository.save(new ChatMessageDocument(
                tripId,
                context.user().getId(),
                context.user().getNickname(),
                context.user().getProfileImageKey(),
                ChatSenderType.USER.name(),
                content,
                Instant.now(),
                seq
        ));

        if (content.contains("@AI")) {
            redisTemplate.opsForStream().add(
                    "stream:ai-jobs",
                    Map.of(
                            "messageId", saved.getId(),
                            "tripId", String.valueOf(tripId)
                    )
            );
            log.info("Published AI job tripId={} messageId={}", tripId, saved.getId());
        }

        context.participant().markRead(seq);
        participantRepository.save(context.participant());

        return toResponse(saved, Map.of(context.user().getId(), context.user()));
    }

    @Transactional
    public ChatSummaryResponse getSummary(Long tripId, String loginId) {
        ChatContext context = getContext(tripId, loginId, false);
        long unread = Math.max(0L, context.chatRoom().getTotalMessageCount() - context.participant().getLastReadCount());
        return new ChatSummaryResponse(unread, context.chatRoom().getTotalMessageCount());
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(Long tripId, String loginId, int limit, Instant before) {
        getContext(tripId, loginId, false);

        int normalizedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        PageRequest pageable = PageRequest.of(0, normalizedLimit);

        List<ChatMessageDocument> messages;
        if (before == null) {
            messages = chatMessageMongoRepository.findByTripIdOrderByCreatedAtDesc(tripId, pageable);
        } else {
            messages = chatMessageMongoRepository.findByTripIdAndCreatedAtBeforeOrderByCreatedAtDesc(tripId, before, pageable);
        }

        Map<Long, User> senderUsersById = getSenderUsersById(messages);
        return messages.stream()
                .map(message -> toResponse(message, senderUsersById))
                .toList();
    }

    @Transactional
    public ChatReadResponse readAll(Long tripId, String loginId) {
        ChatContext context = getContext(tripId, loginId, true);
        long total = context.chatRoom().getTotalMessageCount();
        context.participant().markRead(total);
        participantRepository.save(context.participant());
        return new ChatReadResponse(0L, total);
    }

    private ChatContext getContext(Long tripId, String loginId, boolean forUpdate) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_001));

        TripAccessService.AccessInfo accessInfo = tripAccessService.requireReadable(trip, loginId);
        User user = accessInfo.user();

        ChatRoom chatRoom = forUpdate
                ? chatRoomRepository.findByTripIdForUpdate(tripId).orElseGet(() -> chatRoomRepository.save(new ChatRoom(tripId)))
                : chatRoomRepository.findByTripId(tripId).orElseGet(() -> chatRoomRepository.save(new ChatRoom(tripId)));

        ChatRoomParticipant participant = participantRepository.findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
                .orElseGet(() -> participantRepository.save(new ChatRoomParticipant(chatRoom, user)));

        return new ChatContext(user, chatRoom, participant);
    }

    private Map<Long, User> getSenderUsersById(List<ChatMessageDocument> messages) {
        Set<Long> senderIds = messages.stream()
                .filter(message -> ChatSenderType.USER.name().equalsIgnoreCase(message.getSenderType()))
                .map(ChatMessageDocument::getSenderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ChatMessageResponse toResponse(ChatMessageDocument document, Map<Long, User> senderUsersById) {
        User sender = senderUsersById.get(document.getSenderUserId());
        String senderNickname = StringUtils.hasText(document.getSenderNickname())
                ? document.getSenderNickname()
                : (sender != null ? sender.getNickname() : null);

        String senderProfileImageUrl = null;
        if (StringUtils.hasText(document.getSenderProfileImageKey())) {
            senderProfileImageUrl = imageUrlResolver.resolve(document.getSenderProfileImageKey());
        } else if (sender != null) {
            senderProfileImageUrl = imageUrlResolver.resolve(sender.getProfileImageKey());
        }

        return new ChatMessageResponse(
                document.getId(),
                document.getTripId(),
                document.getSenderUserId(),
                senderNickname,
                senderProfileImageUrl,
                document.getSenderType(),
                document.getContent(),
                document.getCreatedAt(),
                document.getSeq()
        );
    }

    private record ChatContext(User user, ChatRoom chatRoom, ChatRoomParticipant participant) {
    }
}
