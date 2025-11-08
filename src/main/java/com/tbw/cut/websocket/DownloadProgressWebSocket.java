package com.tbw.cut.websocket;

import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.service.VideoDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DownloadProgressWebSocket extends TextWebSocketHandler {
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    // 存储所有活跃的WebSocket会话
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    /**
     * 向所有客户端推送下载进度更新
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    public static void broadcastProgressUpdate(Long taskId, Integer progress) {
        String message = String.format("{\"type\":\"progress\",\"taskId\":%d,\"progress\":%d}", taskId, progress);
        log.info("Broadcasting progress update: taskId={}, progress={}", taskId, progress);
        broadcastMessage(message);
    }
    
    /**
     * 向所有客户端推送任务状态更新
     * @param taskId 任务ID
     * @param status 状态
     */
    public static void broadcastStatusUpdate(Long taskId, Integer status) {
        String message = String.format("{\"type\":\"status\",\"taskId\":%d,\"status\":%d}", taskId, status);
        log.info("Broadcasting status update: taskId={}, status={}", taskId, status);
        broadcastMessage(message);
    }
    
    /**
     * 向所有客户端广播消息
     * @param message 消息内容
     */
    private static void broadcastMessage(String message) {
        log.info("Broadcasting message to {} sessions: {}", sessions.size(), message);
        if (sessions.isEmpty()) {
            log.warn("No active WebSocket sessions to broadcast to");
            return;
        }
        
        int successCount = 0;
        int errorCount = 0;
        
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    log.debug("Message sent to session: {}", session.getId());
                    successCount++;
                } else {
                    log.warn("Session is closed: {}", session.getId());
                    errorCount++;
                }
            } catch (IOException e) {
                log.error("向客户端发送消息失败，会话ID: {}", session.getId(), e);
                errorCount++;
            }
        }
        
        log.info("Message broadcast completed: {} successful, {} errors", successCount, errorCount);
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket连接已建立，会话ID: {}，当前会话数: {}", session.getId(), sessions.size());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket连接已关闭，会话ID: {}，当前会话数: {}", session.getId(), sessions.size());
    }
}