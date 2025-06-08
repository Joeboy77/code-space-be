package com.code_space.code_space.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * WebSocket Security Configuration
 * Allows both authenticated users and guest users to connect to WebSocket endpoints
 */
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    /**
     * Configure message-level security
     * This allows guest users to connect to WebSocket endpoints
     */
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        // Allow all WebSocket connections (both authenticated and guest)
        messages
                // Allow all users to connect and subscribe to topics
                .simpDestMatchers("/app/**").permitAll()
                .simpDestMatchers("/topic/**").permitAll()
                .simpDestMatchers("/user/**").permitAll()
                .simpDestMatchers("/queue/**").permitAll()

                // Allow specific WebRTC endpoints for all users
                .simpDestMatchers("/app/webrtc/**").permitAll()
                .simpDestMatchers("/topic/webrtc/**").permitAll()

                // Allow subscription to session info and other endpoints
                .simpSubscribeDestMatchers("/app/webrtc/**").permitAll()
                .simpSubscribeDestMatchers("/topic/**").permitAll()
                .simpSubscribeDestMatchers("/user/**").permitAll()

                // Require authentication for administrative functions
                .simpDestMatchers("/app/admin/**").authenticated()

                // Allow anything else
                .anyMessage().permitAll();
    }

    /**
     * Disable CSRF for WebSocket connections to allow guest access
     */
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }

    /**
     * Custom channel interceptor to handle both authenticated and guest users
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 99)
    public ChannelInterceptor securityChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    // Handle CONNECT command
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        handleWebSocketConnect(accessor);
                    }

                    // Handle MESSAGE command for guest users
                    if (StompCommand.SEND.equals(accessor.getCommand())) {
                        handleWebSocketMessage(accessor);
                    }
                }

                return message;
            }
        };
    }

    /**
     * Handle WebSocket connection - set up session for guest users
     */
    private void handleWebSocketConnect(StompHeaderAccessor accessor) {
        try {
            // Check if user is authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null
                    && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                    && !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
                // User is authenticated, no additional setup needed
                return;
            }

            // For guest users, create a guest session identifier
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                // Store guest information in session attributes
                accessor.getSessionAttributes().put("isGuest", true);
                accessor.getSessionAttributes().put("guestId", "guest_" + sessionId);
                accessor.getSessionAttributes().put("connectedAt", System.currentTimeMillis());
            }

        } catch (Exception e) {
            // Log error but don't prevent connection
            System.err.println("Error setting up guest session: " + e.getMessage());
        }
    }

    /**
     * Handle WebSocket messages - ensure proper context for guest users
     */
    private void handleWebSocketMessage(StompHeaderAccessor accessor) {
        try {
            // If no authentication context exists, this might be a guest user
            if (SecurityContextHolder.getContext().getAuthentication() == null
                    || "anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getName())) {

                // For guest users, ensure session attributes are maintained
                if (accessor.getSessionAttributes() != null && !accessor.getSessionAttributes().containsKey("isGuest")) {
                    String sessionId = accessor.getSessionId();
                    if (sessionId != null) {
                        accessor.getSessionAttributes().put("isGuest", true);
                        accessor.getSessionAttributes().put("guestId", "guest_" + sessionId);
                    }
                }
            }

        } catch (Exception e) {
            // Log error but don't prevent message processing
            System.err.println("Error handling guest message context: " + e.getMessage());
        }
    }
}