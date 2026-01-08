package com.tbw.cut.service.download.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 智能重试管理器
 * 根据不同的错误类型采用不同的重试策略
 */
@Slf4j
@Component
public class RetryManager {
    
    private final Map<Class<? extends Exception>, RetryStrategy> retryStrategies = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeStrategies() {
        // 连接异常：指数退避重试
        retryStrategies.put(ConnectException.class, 
            new ExponentialBackoffStrategy(3, 1000, 2.0));
        
        // 超时异常：线性退避重试
        retryStrategies.put(SocketTimeoutException.class, 
            new LinearBackoffStrategy(5, 2000));
        
        // DNS解析失败：立即重试
        retryStrategies.put(UnknownHostException.class, 
            new ImmediateRetryStrategy(2));
        
        // HTTP重试异常：条件重试
        retryStrategies.put(HttpRetryException.class, 
            new ConditionalRetryStrategy());
        
        log.info("Initialized retry strategies for {} exception types", retryStrategies.size());
    }
    
    /**
     * 执行带重试的操作
     * @param operation 要执行的操作
     * @param operationName 操作名称（用于日志）
     * @return 操作结果
     */
    public <T> CompletableFuture<T> executeWithRetry(Supplier<T> operation, String operationName) {
        return CompletableFuture.supplyAsync(() -> {
            int attempt = 0;
            Exception lastException = null;
            
            while (attempt < getMaxRetries()) {
                try {
                    T result = operation.get();
                    if (attempt > 0) {
                        log.info("Operation {} succeeded on attempt {}", operationName, attempt + 1);
                    }
                    return result;
                    
                } catch (Exception e) {
                    lastException = e;
                    RetryStrategy strategy = getRetryStrategy(e);
                    
                    if (!strategy.shouldRetry(attempt, e)) {
                        log.warn("Operation {} failed and retry strategy says not to retry: {}", 
                            operationName, e.getMessage());
                        break;
                    }
                    
                    long delay = strategy.getDelayMs(attempt);
                    log.info("Operation {} failed (attempt {}), retrying in {}ms: {}", 
                        operationName, attempt + 1, delay, e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation interrupted", ie);
                    }
                    
                    attempt++;
                }
            }
            
            throw new RetryExhaustedException(
                "Operation " + operationName + " failed after " + attempt + " attempts", lastException);
        });
    }
    
    /**
     * 获取重试策略
     * @param exception 异常
     * @return 重试策略
     */
    private RetryStrategy getRetryStrategy(Exception exception) {
        // 查找精确匹配的策略
        RetryStrategy strategy = retryStrategies.get(exception.getClass());
        if (strategy != null) {
            return strategy;
        }
        
        // 查找父类匹配的策略
        for (Map.Entry<Class<? extends Exception>, RetryStrategy> entry : retryStrategies.entrySet()) {
            if (entry.getKey().isAssignableFrom(exception.getClass())) {
                return entry.getValue();
            }
        }
        
        // 默认策略
        return new DefaultRetryStrategy();
    }
    
    /**
     * 获取最大重试次数
     * @return 最大重试次数
     */
    private int getMaxRetries() {
        return 5; // 默认最大重试5次
    }
    
    /**
     * 重试策略接口
     */
    public interface RetryStrategy {
        boolean shouldRetry(int attempt, Exception exception);
        long getDelayMs(int attempt);
    }
    
    /**
     * 指数退避策略
     */
    public static class ExponentialBackoffStrategy implements RetryStrategy {
        private final int maxRetries;
        private final long baseDelayMs;
        private final double multiplier;
        
        public ExponentialBackoffStrategy(int maxRetries, long baseDelayMs, double multiplier) {
            this.maxRetries = maxRetries;
            this.baseDelayMs = baseDelayMs;
            this.multiplier = multiplier;
        }
        
        @Override
        public boolean shouldRetry(int attempt, Exception exception) {
            return attempt < maxRetries;
        }
        
        @Override
        public long getDelayMs(int attempt) {
            return (long) (baseDelayMs * Math.pow(multiplier, attempt));
        }
    }
    
    /**
     * 线性退避策略
     */
    public static class LinearBackoffStrategy implements RetryStrategy {
        private final int maxRetries;
        private final long delayMs;
        
        public LinearBackoffStrategy(int maxRetries, long delayMs) {
            this.maxRetries = maxRetries;
            this.delayMs = delayMs;
        }
        
        @Override
        public boolean shouldRetry(int attempt, Exception exception) {
            return attempt < maxRetries;
        }
        
        @Override
        public long getDelayMs(int attempt) {
            return delayMs * (attempt + 1);
        }
    }
    
    /**
     * 立即重试策略
     */
    public static class ImmediateRetryStrategy implements RetryStrategy {
        private final int maxRetries;
        
        public ImmediateRetryStrategy(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        @Override
        public boolean shouldRetry(int attempt, Exception exception) {
            return attempt < maxRetries;
        }
        
        @Override
        public long getDelayMs(int attempt) {
            return 0; // 立即重试
        }
    }
    
    /**
     * 条件重试策略
     */
    public static class ConditionalRetryStrategy implements RetryStrategy {
        
        @Override
        public boolean shouldRetry(int attempt, Exception exception) {
            if (attempt >= 3) {
                return false;
            }
            
            // 根据异常类型和消息决定是否重试
            if (exception instanceof HttpRetryException) {
                HttpRetryException httpException = (HttpRetryException) exception;
                int statusCode = httpException.getStatusCode();
                
                // 4xx错误通常不应该重试
                if (statusCode >= 400 && statusCode < 500) {
                    return false;
                }
                
                // 5xx错误可以重试
                return statusCode >= 500 && statusCode < 600;
            }
            
            return true;
        }
        
        @Override
        public long getDelayMs(int attempt) {
            return 1000 * (attempt + 1); // 1s, 2s, 3s
        }
    }
    
    /**
     * 默认重试策略
     */
    public static class DefaultRetryStrategy implements RetryStrategy {
        
        @Override
        public boolean shouldRetry(int attempt, Exception exception) {
            return attempt < 2; // 默认最多重试2次
        }
        
        @Override
        public long getDelayMs(int attempt) {
            return 1000; // 固定1秒延迟
        }
    }
    
    /**
     * HTTP重试异常
     */
    public static class HttpRetryException extends Exception {
        private final int statusCode;
        
        public HttpRetryException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
    }
    
    /**
     * 重试耗尽异常
     */
    public static class RetryExhaustedException extends RuntimeException {
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}