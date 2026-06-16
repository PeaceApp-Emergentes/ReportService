package com.upc.pre.peaceapp.reports.interfaces.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.reports.interfaces.ws.resources.EmergencyResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw WebSocket handler that keeps track of the municipality dashboards
 * connected to /ws/alerts and broadcasts emergency reports to all of them in
 * real time. Uses a plain JSON text frame (no STOMP) so the web client can read
 * it with the native WebSocket API.
 */
@Component
@Slf4j
public class EmergencyWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Emergency WS client connected: {} (total {})", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("Emergency WS client disconnected: {} (total {})", session.getId(), sessions.size());
    }

    /**
     * Serializes the emergency and pushes it to every connected dashboard.
     */
    public void broadcast(EmergencyResource emergency) {
        if (sessions.isEmpty()) {
            log.info("No emergency WS clients connected; skipping broadcast for report {}", emergency.reportId());
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(emergency);
            TextMessage message = new TextMessage(payload);
            sessions.values().forEach(session -> sendSafely(session, message));
            log.info("Broadcast emergency for report {} to {} client(s)", emergency.reportId(), sessions.size());
        } catch (Exception e) {
            log.error("Failed to serialize emergency for report {}: {}", emergency.reportId(), e.getMessage());
        }
    }

    private void sendSafely(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to send emergency to session {}: {}", session.getId(), e.getMessage());
            sessions.remove(session.getId());
        }
    }
}
