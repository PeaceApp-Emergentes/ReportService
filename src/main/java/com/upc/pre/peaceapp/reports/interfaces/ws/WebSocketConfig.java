package com.upc.pre.peaceapp.reports.interfaces.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the raw WebSocket endpoint used by the municipality dashboard to
 * receive emergency reports in real time.
 *
 * The web client connects directly to this service (default port 8083) at
 * ws://localhost:8083/ws/alerts. Routing WebSockets through Spring Cloud Gateway
 * is intentionally avoided here for simplicity, so the dashboard must point to
 * the ReportService host/port directly (see PeaceApp-Web environment.js -> websocketUrl).
 *
 * CORS-style allowed origins are configured for the Vite dev server. Adjust /
 * widen as needed for other environments.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final EmergencyWebSocketHandler emergencyWebSocketHandler;

    public WebSocketConfig(EmergencyWebSocketHandler emergencyWebSocketHandler) {
        this.emergencyWebSocketHandler = emergencyWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(emergencyWebSocketHandler, "/ws/alerts")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "https://peaceapp.duckdns.org"
                );
    }
}
