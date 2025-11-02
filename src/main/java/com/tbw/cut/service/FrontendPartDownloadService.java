package com.tbw.cut.service;

import com.tbw.cut.dto.VideoDownloadDTO;

import java.util.Map;
import java.util.List;

public interface FrontendPartDownloadService {
    
    /**
     * 处理前端传递的parts信息下载
     * @param taskId 任务ID
     * @param videoUrl 视频URL
     * @param bvid BV ID
     * @param aid AV ID
     * @param parts 前端传递的parts信息
     * @param config 下载配置
     */
    void downloadParts(Long taskId, String videoUrl, String bvid, String aid, 
                      List<Map<String, Object>> parts, Map<String, Object> config);
}