package com.samjay.ai_service.configurations;

import com.samjay.ai_service.services.interfaces.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

import static com.samjay.ai_service.utility.AppExtensions.AUTH_ATTRIBUTE;
import static com.samjay.ai_service.utility.AppExtensions.USER_ID_CLAIM_KEY;

@SuppressWarnings("NullableProblems")
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionRegistry webSocketSessionRegistry;

    private final AiService aiService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        JwtAuthenticationToken auth = (JwtAuthenticationToken) session.getAttributes()
                .get(AUTH_ATTRIBUTE);

        String userId = auth.getToken().getClaim(USER_ID_CLAIM_KEY);

        log.info("User connected: session={}, userId={}", session.getId(), userId);

        webSocketSessionRegistry.register(UUID.fromString(userId), session);

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        JwtAuthenticationToken auth = (JwtAuthenticationToken) session.getAttributes()
                .get(AUTH_ATTRIBUTE);

        String userId = auth.getToken().getClaim(USER_ID_CLAIM_KEY);

        try {

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(auth);

            SecurityContextHolder.setContext(securityContext);

            String userPrompt = message.getPayload();

            String aiResponse = aiService.sendMessageV2(userPrompt, userId);

            session.sendMessage(new TextMessage(aiResponse));

        } catch (Exception e) {

            log.error("Failed to update location for driver {}: {}", userId, e.getMessage(), e);

            session.sendMessage(new TextMessage("An error occurred while processing your request."));

        } finally {

            SecurityContextHolder.clearContext();

        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        log.info("User disconnected: session={}, reason={}", session.getId(), status.getReason());

        UUID userId = webSocketSessionRegistry.unregister(session);

        log.info("User disconnected={}: session={}, reason={}", userId, session.getId(), status.getReason());

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {

        log.error("Transport error for driver session {}: {}", session.getId(), exception.getMessage());

    }
}
