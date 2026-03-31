package com.samjay.driver_service.configurations;

import com.samjay.driver_service.dtos.responses.LocationUpdateResponse;
import com.samjay.driver_service.dtos.websocket.DriverLocationMessage;
import com.samjay.driver_service.services.interfaces.H3DriverMatchingService;
import com.samjay.driver_service.services.interfaces.RedisDriverLocationStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static com.samjay.driver_service.utility.AppExtensions.AUTH_ATTRIBUTE;
import static com.samjay.driver_service.utility.AppExtensions.USER_ID_CLAIM_KEY;

@SuppressWarnings("NullableProblems")
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverWebSocketHandler extends TextWebSocketHandler {

    private final H3DriverMatchingService h3DriverMatchingService;

    private final RedisDriverLocationStore redisDriverLocationStore;

    private final DriverSessionRegistry driverSessionRegistry;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        JwtAuthenticationToken auth = (JwtAuthenticationToken) session.getAttributes()
                .get(AUTH_ATTRIBUTE);

        String userId = auth.getToken().getClaim(USER_ID_CLAIM_KEY);

        log.info("Driver connected: session={}, userId={}", session.getId(), userId);

        driverSessionRegistry.register(UUID.fromString(userId), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        JwtAuthenticationToken auth = (JwtAuthenticationToken) session.getAttributes()
                .get(AUTH_ATTRIBUTE);

        String userId = auth.getToken().getClaim(USER_ID_CLAIM_KEY);

        try {

            DriverLocationMessage msg = objectMapper.readValue(message.getPayload(), DriverLocationMessage.class);

            h3DriverMatchingService.updateDriverLocation(
                    UUID.fromString(userId),
                    msg.getLatitude(),
                    msg.getLongitude()
            );

            LocationUpdateResponse locationUpdateSucceeded = new LocationUpdateResponse(200, "Location updated");

            String json = objectMapper.writeValueAsString(locationUpdateSucceeded);

            session.sendMessage(new TextMessage(json));

        } catch (Exception e) {

            log.error("Failed to update location for driver {}: {}", userId, e.getMessage(), e);

            LocationUpdateResponse locationUpdateFailed = new LocationUpdateResponse(500, "Location update failed");

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(locationUpdateFailed)));

        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        log.info("Driver disconnected: session={}, reason={}", session.getId(), status.getReason());

        UUID userId = driverSessionRegistry.unregister(session);

        if (userId != null) {

            redisDriverLocationStore.removeDriver(userId);

            log.info("Removed driver with user Id: {} from Redis after disconnection", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {

        log.error("Transport error for driver session {}: {}", session.getId(), exception.getMessage());
    }
}
