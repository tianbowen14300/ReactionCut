package com.tbw.cut.service;

import com.tbw.cut.dto.VideoDownloadDTO;

import java.util.Map;

public interface FrontendVideoDownloadService {
    
    /**
     * 处理前端发送的下载请求
     * @param requestData 前端发送的请求数据
     * @return 任务ID
     */
    Long handleFrontendDownloadRequest(Map<String, Object> requestData);
    
    /**
     * 转换前端DTO格式到后端格式
     * @param requestData 前端发送的请求数据
     * @return 后端使用的VideoDownloadDTO
     */
    VideoDownloadDTO convertToBackendDTO(Map<String, Object> requestData);
}