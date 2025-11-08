package com.tbw.cut.entity;

import java.util.List;
import java.util.Map;

/**
 * 下载任务实体类
 */
public class DownloadTask extends VideoDownload {
    private String videoUrl;
    private String bvid;
    private String aid;
    private List<Map<String, Object>> parts;
    private Map<String, Object> config;
    
    public DownloadTask() {
        super();
    }
    
    public DownloadTask(Long taskId, String videoUrl, String bvid, String aid,
                       List<Map<String, Object>> parts, Map<String, Object> config) {
        this.setId(taskId);
        this.videoUrl = videoUrl;
        this.bvid = bvid;
        this.aid = aid;
        this.parts = parts;
        this.config = config;
    }
    
    // Getters and Setters
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    
    public String getBvid() { return bvid; }
    public void setBvid(String bvid) { this.bvid = bvid; }
    
    public String getAid() { return aid; }
    public void setAid(String aid) { this.aid = aid; }
    
    public List<Map<String, Object>> getParts() { return parts; }
    public void setParts(List<Map<String, Object>> parts) { this.parts = parts; }
    
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
}