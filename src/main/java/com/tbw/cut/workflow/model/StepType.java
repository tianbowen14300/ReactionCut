package com.tbw.cut.workflow.model;

/**
 * 工作流步骤类型枚举
 */
public enum StepType {
    CLIPPING("视频剪辑"),
    MERGING("视频合并"),
    SEGMENTATION("视频分段"),
    SEGMENTING("视频分段处理"),
    SUBMISSION("视频投稿"),
    DOWNLOADING("视频下载"),
    UPLOADING("视频上传"),
    VALIDATION("数据验证");
    
    private final String description;
    
    StepType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}