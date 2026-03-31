package com.samjay.driver_service.configurations;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static com.samjay.driver_service.utility.AppExtensions.AUTH_ATTRIBUTE;
import static com.samjay.driver_service.utility.AppExtensions.BEARER_PREFIX;

@SuppressWarnings("NullableProblems")
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletRequest))
            return false;

        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX))
            return false;

        String token = authHeader.substring(7);

        if (token.isEmpty())
            return false;

        try {

            Jwt jwt = jwtDecoder.decode(token);

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

            attributes.put(AUTH_ATTRIBUTE, authentication);

            log.info("WebSocket handshake accepted for subject: {}", jwt.getSubject());

            return true;

        } catch (JwtException e) {

            log.warn("WebSocket handshake rejected: invalid token — {}", e.getMessage());

            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               @Nullable Exception exception) {

    }
}
