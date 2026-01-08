package com.tbw.cut.service.download.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 下载时长日志记录器测试
 */
class DownloadTimeLoggerTest {
    
    private static final Logger log = LoggerFactory.getLogger(DownloadTimeLoggerTest.class);
    
    private DownloadTimeLogger downloadTimeLogger;
    
    @BeforeEach
    void setUp() {
        downloadTimeLogger = new DownloadTimeLogger();
    }
    
    @Test
    void testDownloadLogging() throws InterruptedException {
        Long taskId = 12345L;
        String videoTitle = "测试视频";
        String videoUrl = "http://example.com/video.mp4";
        long fileSize = 100 * 1024 * 1024; // 100MB
        int segmentCount = 4;
        
        log.info("=== 开始测试下载日志记录功能 ===");
        
        // 1. 记录下载开始
        downloadTimeLogger.logDownloadStart(taskId, videoTitle, videoUrl, fileSize, segmentCount);
        
        // 2. 模拟分段下载
        for (int i = 0; i < segmentCount; i++) {
            long segmentSize = fileSize / segmentCount;
            long segmentDuration = 500 + (i * 100); // 模拟不同的下载时长
            
            downloadTimeLogger.logSegmentDownloadTime(taskId, i, segmentSize, segmentDuration);
            Thread.sleep(10); // 短暂延迟
        }
        
        // 3. 记录文件合并
        long mergeTime = 200;
        downloadTimeLogger.logFileMergeTime(taskId, segmentCount, fileSize, mergeTime);
        
        // 4. 记录下载完成
        Thread.sleep(100); // 确保有一些下载时间
        downloadTimeLogger.logDownloadComplete(taskId, true, null, fileSize);
        
        // 5. 显示统计信息
        String statistics = downloadTimeLogger.getDownloadStatistics();
        log.info("下载统计信息: {}", statistics);
        
        assertNotNull(statistics);
        assertTrue(statistics.contains("总下载: 1"));
        assertTrue(statistics.contains("成功: 1"));
        
        log.info("=== 下载日志记录功能测试完成 ===");
    }
    
    @Test
    void testMultipleDownloads() throws InterruptedException {
        log.info("=== 开始测试多个下载任务的日志记录 ===");
        
        // 模拟多个下载任务
        for (int i = 0; i < 3; i++) {
            Long taskId = (long) (1000 + i);
            String videoTitle = "测试视频 " + (i + 1);
            String videoUrl = "http://example.com/video" + i + ".mp4";
            long fileSize = (50 + i * 25) * 1024 * 1024; // 不同大小的文件
            
            // 记录下载开始
            downloadTimeLogger.logDownloadStart(taskId, videoTitle, videoUrl, fileSize, 2);
            
            // 模拟下载过程
            Thread.sleep(50);
            
            // 记录下载完成（模拟一个失败的下载）
            boolean success = i != 1; // 第二个下载失败
            String errorMessage = success ? null : "网络连接超时";
            downloadTimeLogger.logDownloadComplete(taskId, success, errorMessage, success ? fileSize : 0);
        }
        
        // 显示最终统计
        String finalStats = downloadTimeLogger.getDownloadStatistics();
        log.info("最终统计信息: {}", finalStats);
        
        assertTrue(finalStats.contains("总下载: 3")); // 3 downloads in this test
        assertTrue(finalStats.contains("成功: 2"));
        assertTrue(finalStats.contains("失败: 1"));
        
        log.info("=== 多个下载任务日志记录测试完成 ===");
    }
    
    @Test
    void testQueueWaitTimeLogging() {
        log.info("=== 开始测试队列等待时长记录 ===");
        
        Long taskId = 9999L;
        
        // 测试短等待时间（不应该记录）
        downloadTimeLogger.logQueueWaitTime(taskId, 500);
        
        // 测试长等待时间（应该记录）
        downloadTimeLogger.logQueueWaitTime(taskId, 2500);
        
        log.info("=== 队列等待时长记录测试完成 ===");
    }
}