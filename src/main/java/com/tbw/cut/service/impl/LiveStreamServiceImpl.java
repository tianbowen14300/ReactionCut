package com.tbw.cut.service.impl;

import com.tbw.cut.entity.Anchor;
import com.tbw.cut.service.LiveStreamService;
import com.tbw.cut.utils.FFmpegUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class LiveStreamServiceImpl implements LiveStreamService {
    
    @Autowired
    private FFmpegUtil ffmpegUtil;
    
    @Override
    public String recordLiveStream(Anchor anchor, String streamUrl) {
        try {
            // Generate output file name with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFileName = "live_" + anchor.getUid() + "_" + timestamp + ".flv";
            
            // Record live stream using FFmpeg
            return ffmpegUtil.recordLiveStream(streamUrl, outputFileName);
        } catch (Exception e) {
            log.error("Failed to record live stream for anchor: {}", anchor.getUid(), e);
            return null;
        }
    }
    
    @Override
    public String getLiveStreamUrl(Anchor anchor) {
        // This would typically call Bilibili's API to get the live stream URL
        // For now, we'll return a placeholder
        log.info("Getting live stream URL for anchor: {}", anchor.getUid());
        
        // In a real implementation, you would:
        // 1. Call Bilibili's API to check if the anchor is live
        // 2. If live, get the stream URL
        // 3. Return the stream URL
        
        // Placeholder implementation
        return "https://live-bilibili.example.com/live/" + anchor.getUid() + ".flv";
    }
}