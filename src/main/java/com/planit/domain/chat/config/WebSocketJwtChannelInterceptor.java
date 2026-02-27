package com.planit.domain.chat.config;

import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.domain.user.security.JwtProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
                throw new AccessDeniedException("Unauthorized websocket connect");
            }

            String token = authorization.substring(7);
            if (!jwtProvider.validateToken(token)) {
                throw new AccessDeniedException("Invalid websocket token");
            }

            String loginId = jwtProvider.getSubject(token);
            Optional<User> optionalUser = userRepository.findByLoginIdAndDeletedFalse(loginId);
            if (optionalUser.isEmpty()) {
                throw new AccessDeniedException("User not found");
            }

            User user = optionalUser.get();
            UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getLoginId())
                    .password(user.getPassword())
                    .authorities("ROLE_USER")
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
        }

        if ((StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))
                && accessor.getUser() == null) {
            throw new AccessDeniedException("Authentication is required for websocket messaging");
        }

        return message;
    }
}
