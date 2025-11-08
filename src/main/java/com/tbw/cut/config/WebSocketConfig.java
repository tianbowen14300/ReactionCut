package com.tbw.cut.config;

import com.tbw.cut.websocket.DownloadProgressWebSocket;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new DownloadProgressWebSocket(), "/ws/download-progress")
                .setAllowedOrigins("http://localhost:8081", "http://localhost:8100", "*"); // 允许前端开发服务器的跨域请求
    }
}