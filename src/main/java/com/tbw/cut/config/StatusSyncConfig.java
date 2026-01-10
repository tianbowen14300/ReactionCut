package com.tbw.cut.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 状态同步配置类
 * 配置异步处理和定时任务
 * 
 * 注意：statusSyncExecutor bean已移至AsyncConfig中统一管理
 */
@Configuration
@EnableAsync
@EnableScheduling
public class StatusSyncConfig {
    // statusSyncExecutor bean已在AsyncConfig中定义，避免重复
}