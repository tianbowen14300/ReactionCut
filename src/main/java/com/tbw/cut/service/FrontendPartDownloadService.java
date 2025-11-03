package com.tbw.cut.service;

import com.tbw.cut.entity.DownloadTask;

import java.util.List;
import java.util.Map;

public interface FrontendPartDownloadService {
    

    /**
     * 添加下载任务到队列
     * @param downloadTask 下载任务
     */
    void addDownloadTask(DownloadTask downloadTask);
    
    /**
     * 执行下载任务
     * @param downloadTask 下载任务
     */
    void executeDownload(DownloadTask downloadTask);
}