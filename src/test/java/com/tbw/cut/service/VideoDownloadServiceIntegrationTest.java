package com.tbw.cut.service;

import com.tbw.cut.dto.VideoDownloadDTO;
import com.tbw.cut.service.impl.VideoDownloadServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class VideoDownloadServiceIntegrationTest {

    @Autowired
    private VideoDownloadService videoDownloadService;

    @Test
    public void testDownloadVideo() {
        // 创建下载任务
        VideoDownloadDTO dto = new VideoDownloadDTO();
        dto.setVideoUrl("https://www.bilibili.com/video/BV1kk1uByEVU");
        
        // 执行下载
        Long taskId = videoDownloadService.downloadVideo(dto);
        
        System.out.println("任务ID: " + taskId);
        
        // 等待一段时间让下载完成
        try {
            Thread.sleep(30000); // 等待30秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 检查任务状态
        if (taskId != null) {
            com.tbw.cut.entity.VideoDownload download = videoDownloadService.getById(taskId);
            System.out.println("任务状态: " + download.getStatus());
            System.out.println("任务进度: " + download.getProgress());
        }
    }
}