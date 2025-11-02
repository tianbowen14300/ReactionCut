package com.tbw.cut.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.download")
public class DownloadConfig {
    /**
     * 多线程下载线程数
     */
    private int threads = 3;
    
    /**
     * 下载队列大小
     */
    private int queueSize = 3;
}