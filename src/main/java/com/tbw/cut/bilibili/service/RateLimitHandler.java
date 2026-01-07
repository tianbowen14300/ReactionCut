package com.tbw.cut.bilibili.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 速率限制处理器
 */
@Slf4j
@Service
public class RateLimitHandler {
    
    // 连续406错误计数器
    private final AtomicInteger consecutive406Count = new AtomicInteger(0);
    
    // 406错误等待时间（毫秒）
    private static final long WAIT_TIME_406 = 30 * 60 * 1000; // 30分钟
    
    // 最大连续406错误次数
    private static final int MAX_406_COUNT = 3;
    
    /**
     * 处理406错误
     * @param errorMessage 错误信息
     * @return 需要等待的时间（毫秒），0表示不需要等待
     */
    public long handle406Error(String errorMessage) {
        if (errorMessage != null && errorMessage.contains("406")) {
            int count = consecutive406Count.incrementAndGet();
            log.warn("检测到406错误，连续406错误次数: {}", count);
            
            if (count <= MAX_406_COUNT) {
                return WAIT_TIME_406;
            } else {
                log.error("连续406错误次数超过最大限制: {}", MAX_406_COUNT);
                return 0; // 超过最大次数，不再等待
            }
        }
        
        // 不是406错误，重置计数器
        consecutive406Count.set(0);
        return 0;
    }
    
    /**
     * 智能等待
     * @param waitTime 等待时间（毫秒）
     * @throws InterruptedException 线程中断异常
     */
    public void smartWait(long waitTime) throws InterruptedException {
        if (waitTime <= 0) {
            return;
        }
        
        log.info("开始等待 {} 毫秒以避免速率限制", waitTime);
        
        // 分段等待，每分钟输出一次日志
        long remainingTime = waitTime;
        long oneMinute = 60 * 1000;
        
        while (remainingTime > 0) {
            long sleepTime = Math.min(remainingTime, oneMinute);
            Thread.sleep(sleepTime);
            remainingTime -= sleepTime;
            
            if (remainingTime > 0) {
                log.info("继续等待，剩余时间: {} 分钟", remainingTime / oneMinute);
            }
        }
        
        log.info("等待结束，继续处理");
    }
    
    /**
     * 获取当前连续406错误次数
     */
    public int getCurrent406Count() {
        return consecutive406Count.get();
    }
    
    /**
     * 重置406错误计数器
     */
    public void reset406Count() {
        int oldCount = consecutive406Count.getAndSet(0);
        if (oldCount > 0) {
            log.info("重置406错误计数器，之前计数: {}", oldCount);
        }
    }
    
    /**
     * 获取等待时间描述
     */
    public String getWaitTimeDescription(long waitTimeMs) {
        if (waitTimeMs <= 0) {
            return "无需等待";
        }
        
        long minutes = waitTimeMs / (60 * 1000);
        long seconds = (waitTimeMs % (60 * 1000)) / 1000;
        
        if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    /**
     * 检查是否应该停止重试
     */
    public boolean shouldStopRetry() {
        return consecutive406Count.get() > MAX_406_COUNT;
    }
}