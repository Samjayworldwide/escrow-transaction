package com.samjay.driver_service.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.samjay.driver_service.utility.AppExtensions.USER_ID_CLAIM_KEY;

@SuppressWarnings("resource")
@Component
@Slf4j
public class DriverSessionRegistry {

    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(UUID userId, WebSocketSession session) {

        sessions.put(userId, session);

        session.getAttributes().put(USER_ID_CLAIM_KEY, userId);
    }

    public UUID unregister(WebSocketSession session) {

        UUID userId = (UUID) session.getAttributes().get(USER_ID_CLAIM_KEY);

        if (userId == null)
            return null;

        sessions.remove(userId);

        session.getAttributes().remove(USER_ID_CLAIM_KEY);

        if (session.isOpen()) {

            try {

                session.close();

            } catch (Exception ex) {

                log.error("Error while closing driver session", ex);

            }
        }

        return userId;

    }

    public WebSocketSession getSession(UUID userId) {

        return sessions.get(userId);

    }
}
