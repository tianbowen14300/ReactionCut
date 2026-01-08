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
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class DownloadProgressWebSocket extends TextWebSocketHandler {
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    // 存储所有活跃的WebSocket会话
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // 为每个会话添加锁，防止并发写入
    private static final Map<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();
    
    // ========== 进度更新节流控制 ==========
    
    // 每个任务的最后更新时间
    private static final Map<Long, Long> lastUpdateTimes = new ConcurrentHashMap<>();
    
    // 每个任务的最后进度值
    private static final Map<Long, Integer> lastProgressValues = new ConcurrentHashMap<>();
    
    // 默认配置值
    private static final long DEFAULT_UPDATE_INTERVAL_MS = 1000; // 1秒
    private static final int DEFAULT_PROGRESS_THRESHOLD = 5; // 5%
    
    /**
     * 向所有客户端推送下载进度更新（带节流控制）
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    public static void broadcastProgressUpdate(Long taskId, Integer progress) {
        broadcastProgressUpdate(taskId, progress, DEFAULT_UPDATE_INTERVAL_MS, DEFAULT_PROGRESS_THRESHOLD, true);
    }
    
    /**
     * 向所有客户端推送下载进度更新（可配置节流参数）
     * @param taskId 任务ID
     * @param progress 进度百分比
     * @param updateIntervalMs 更新间隔（毫秒）
     * @param progressThreshold 进度变化阈值（百分比）
     * @param enableThrottling 是否启用节流
     */
    public static void broadcastProgressUpdate(Long taskId, Integer progress, 
                                             long updateIntervalMs, int progressThreshold, boolean enableThrottling) {
        
        if (!enableThrottling) {
            // 不启用节流，直接发送
            sendProgressUpdate(taskId, progress);
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        Long lastUpdateTime = lastUpdateTimes.get(taskId);
        Integer lastProgress = lastProgressValues.get(taskId);
        
        boolean shouldUpdate = false;
        
        // 检查是否应该更新
        if (lastUpdateTime == null || lastProgress == null) {
            // 首次更新
            shouldUpdate = true;
        } else if (progress >= 100) {
            // 完成时总是更新
            shouldUpdate = true;
        } else if (progress == 0) {
            // 重置时总是更新
            shouldUpdate = true;
        } else if (currentTime - lastUpdateTime >= updateIntervalMs) {
            // 时间间隔达到
            shouldUpdate = true;
        } else if (Math.abs(progress - lastProgress) >= progressThreshold) {
            // 进度变化达到阈值
            shouldUpdate = true;
        }
        
        if (shouldUpdate) {
            lastUpdateTimes.put(taskId, currentTime);
            lastProgressValues.put(taskId, progress);
            sendProgressUpdate(taskId, progress);
            log.debug("Progress update sent: taskId={}, progress={}, lastProgress={}, timeSinceLastUpdate={}ms", 
                     taskId, progress, lastProgress, lastUpdateTime != null ? (currentTime - lastUpdateTime) : 0);
        } else {
            log.trace("Progress update throttled: taskId={}, progress={}, lastProgress={}, timeSinceLastUpdate={}ms", 
                     taskId, progress, lastProgress, currentTime - lastUpdateTime);
        }
    }
    
    /**
     * 实际发送进度更新消息
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    private static void sendProgressUpdate(Long taskId, Integer progress) {
        String message = String.format("{\"type\":\"progress\",\"taskId\":%d,\"progress\":%d}", taskId, progress);
        log.info("Broadcasting progress update: taskId={}, progress={}, sessions={}", taskId, progress, sessions.size());
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
        
        // 状态更新时清理节流数据（任务完成或失败时）
        if (status == 2 || status == 3) { // 完成或失败
            clearThrottlingData(taskId);
        }
    }
    
    /**
     * 清理任务的节流数据
     * @param taskId 任务ID
     */
    public static void clearThrottlingData(Long taskId) {
        lastUpdateTimes.remove(taskId);
        lastProgressValues.remove(taskId);
        log.debug("Cleared throttling data for task: {}", taskId);
    }
    
    /**
     * 强制发送进度更新（忽略节流）
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    public static void forceProgressUpdate(Long taskId, Integer progress) {
        lastUpdateTimes.put(taskId, System.currentTimeMillis());
        lastProgressValues.put(taskId, progress);
        sendProgressUpdate(taskId, progress);
    }
    
    /**
     * 向所有客户端广播消息（线程安全版本）
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
        
        // 创建会话快照，避免并发修改异常
        Map<String, WebSocketSession> sessionSnapshot = new ConcurrentHashMap<>(sessions);
        
        for (Map.Entry<String, WebSocketSession> entry : sessionSnapshot.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();
            
            // 获取会话锁
            ReentrantLock sessionLock = sessionLocks.get(sessionId);
            if (sessionLock == null) {
                continue; // 会话可能已经被移除
            }
            
            try {
                // 尝试获取锁，避免长时间阻塞
                if (sessionLock.tryLock()) {
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(message));
                            log.debug("Message sent to session: {}", sessionId);
                            successCount++;
                        } else {
                            log.warn("Session is closed, removing: {}", sessionId);
                            removeSession(sessionId);
                            errorCount++;
                        }
                    } finally {
                        sessionLock.unlock();
                    }
                } else {
                    log.warn("Could not acquire lock for session: {}, skipping message", sessionId);
                    errorCount++;
                }
            } catch (IOException e) {
                log.error("Failed to send message to session: {}, removing session", sessionId, e);
                removeSession(sessionId);
                errorCount++;
            } catch (Exception e) {
                log.error("Unexpected error sending message to session: {}", sessionId, e);
                errorCount++;
            }
        }
        
        log.info("Message broadcast completed: {} successful, {} errors", successCount, errorCount);
    }
    
    /**
     * 安全地移除会话
     * @param sessionId 会话ID
     */
    private static void removeSession(String sessionId) {
        sessions.remove(sessionId);
        ReentrantLock lock = sessionLocks.remove(sessionId);
        if (lock != null) {
            // 确保锁被释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        sessionLocks.put(sessionId, new ReentrantLock());
        log.info("WebSocket连接已建立，会话ID: {}，当前会话数: {}", sessionId, sessions.size());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        removeSession(sessionId);
        log.info("WebSocket连接已关闭，会话ID: {}，当前会话数: {}", sessionId, sessions.size());
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket transport error for session: {}", sessionId, exception);
        removeSession(sessionId);
    }
}