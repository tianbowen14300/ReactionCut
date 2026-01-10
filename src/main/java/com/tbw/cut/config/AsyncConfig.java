package com.tbw.cut.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置
 * 
 * 配置工作流事件处理的线程池和其他异步任务
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {
    
    /**
     * 工作流事件处理线程池
     * 
     * @return 工作流事件执行器
     */
    @Bean(name = "workflowEventExecutor")
    public Executor workflowEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：处理工作流事件的基本线程数
        executor.setCorePoolSize(2);
        
        // 最大线程数：高峰期可以扩展到的最大线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量：等待处理的任务队列大小
        executor.setQueueCapacity(100);
        
        // 线程名前缀：便于日志追踪和问题排查
        executor.setThreadNamePrefix("workflow-event-");
        
        // 线程空闲时间：超过核心线程数的线程在空闲多长时间后被回收
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：当线程池和队列都满时的处理策略
        // CallerRunsPolicy：由调用线程执行任务，提供优雅的降级
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：关闭线程池时等待任务完成的最大时间
        executor.setAwaitTerminationSeconds(60);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("工作流事件线程池已配置: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 通用异步任务线程池
     * 
     * @return 通用异步执行器
     */
    @Bean(name = "generalAsyncExecutor")
    public Executor generalAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 通用异步任务的线程池配置
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("general-async-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("通用异步线程池已配置: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 下载事件处理线程池
     * 
     * @return 下载事件执行器
     */
    @Bean(name = "downloadEventExecutor")
    public Executor downloadEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 下载事件处理的线程池配置
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(150);
        executor.setThreadNamePrefix("download-event-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("下载事件线程池已配置: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 状态同步线程池
     * 
     * @return 状态同步执行器
     */
    @Bean(name = "statusSyncExecutor")
    public Executor statusSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 状态同步的线程池配置
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(80);
        executor.setThreadNamePrefix("status-sync-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("状态同步线程池已配置: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}