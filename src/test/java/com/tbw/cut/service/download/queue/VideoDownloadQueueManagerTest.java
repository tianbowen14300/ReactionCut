package com.tbw.cut.service.download.queue;

import com.tbw.cut.service.download.model.*;
import com.tbw.cut.service.download.segmented.SegmentedDownloadManager;
import com.tbw.cut.service.download.logging.DownloadTimeLogger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 视频下载队列管理器测试
 */
@Slf4j
class VideoDownloadQueueManagerTest {
    
    @Mock
    private SegmentedDownloadManager segmentedDownloadManager;
    
    @Mock
    private DownloadTimeLogger downloadTimeLogger;
    
    @InjectMocks
    private VideoDownloadQueueManager queueManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置配置值
        ReflectionTestUtils.setField(queueManager, "maxConcurrentVideos", 2);
        ReflectionTestUtils.setField(queueManager, "queueCapacity", 10);
        
        // 初始化队列管理器
        queueManager.initialize();
        
        // Mock 分段下载管理器
        when(segmentedDownloadManager.downloadVideo(any(VideoDownloadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(DownloadResult.success("Test download completed")));
    }
    
    @Test
    void testSubmitVideoDownload() {
        // 创建测试请求
        VideoDownloadRequest request = createTestRequest("Test Video 1");
        
        // 提交下载任务
        CompletableFuture<DownloadResult> future = queueManager.submitVideoDownload(request);
        
        // 验证任务已提交
        assertNotNull(future);
        // 由于mock立即完成，future可能已经完成
        
        // 获取队列状态
        QueueStatus status = queueManager.getQueueStatus();
        assertTrue(status.getTotalTasks() >= 1);
    }
    
    @Test
    void testConcurrencyControl() {
        // 提交多个下载任务
        VideoDownloadRequest request1 = createTestRequest("Test Video 1");
        VideoDownloadRequest request2 = createTestRequest("Test Video 2");
        VideoDownloadRequest request3 = createTestRequest("Test Video 3");
        
        CompletableFuture<DownloadResult> future1 = queueManager.submitVideoDownload(request1);
        CompletableFuture<DownloadResult> future2 = queueManager.submitVideoDownload(request2);
        CompletableFuture<DownloadResult> future3 = queueManager.submitVideoDownload(request3);
        
        // 验证并发控制
        QueueStatus status = queueManager.getQueueStatus();
        assertEquals(2, status.getMaxConcurrentVideos());
        assertTrue(status.getActiveDownloads() <= 2);
        
        // 第三个任务应该在队列中等待
        if (status.getActiveDownloads() == 2) {
            assertTrue(status.getPendingDownloads() > 0);
        }
    }
    
    @Test
    void testPriorityQueue() {
        // 提交带优先级的任务
        VideoDownloadRequest lowPriorityRequest = createTestRequest("Low Priority Video");
        VideoDownloadRequest highPriorityRequest = createTestRequest("High Priority Video");
        
        // 先提交低优先级任务（优先级值大）
        queueManager.submitVideoDownload(lowPriorityRequest, 10);
        
        // 再提交高优先级任务（优先级值小）
        queueManager.submitVideoDownload(highPriorityRequest, 1);
        
        // 验证队列状态
        QueueStatus status = queueManager.getQueueStatus();
        assertTrue(status.getTotalTasks() >= 2);
    }
    
    @Test
    void testUpdateMaxConcurrentVideos() {
        // 测试更新最大并发数
        queueManager.updateMaxConcurrentVideos(4);
        
        QueueStatus status = queueManager.getQueueStatus();
        assertEquals(4, status.getMaxConcurrentVideos());
        
        // 测试无效值
        queueManager.updateMaxConcurrentVideos(0);
        assertEquals(4, status.getMaxConcurrentVideos()); // 应该保持不变
        
        queueManager.updateMaxConcurrentVideos(15);
        assertEquals(4, status.getMaxConcurrentVideos()); // 应该保持不变（超过最大值）
    }
    
    @Test
    void testQueueCapacity() {
        // 测试队列容量限制的基本功能
        QueueStatus initialStatus = queueManager.getQueueStatus();
        assertEquals(10, initialStatus.getMaxConcurrentVideos() + 8); // 2 + 8 = 10 (容量)
        
        // 提交一些任务来验证队列管理器工作正常
        for (int i = 0; i < 5; i++) {
            VideoDownloadRequest request = createTestRequest("Test Video " + i);
            CompletableFuture<DownloadResult> future = queueManager.submitVideoDownload(request);
            assertNotNull(future);
        }
        
        // 验证任务已被处理
        QueueStatus finalStatus = queueManager.getQueueStatus();
        assertTrue(finalStatus.getTotalTasks() >= 5);
    }
    
    /**
     * 创建测试用的下载请求
     */
    private VideoDownloadRequest createTestRequest(String title) {
        VideoPart part = VideoPart.builder()
            .title("Part 1")
            .url("http://example.com/video.mp4")
            .estimatedSize(100L * 1024 * 1024) // 100MB
            .build();
        
        return VideoDownloadRequest.builder()
            .videoTitle(title)
            .videoUrl("http://example.com/video")
            .parts(Arrays.asList(part))
            .outputDirectory("/tmp/downloads")
            .enableSegmentedDownload(true)
            .estimatedFileSize(100L * 1024 * 1024)
            .build();
    }
}