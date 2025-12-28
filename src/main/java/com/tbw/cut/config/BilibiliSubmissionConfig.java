package com.tbw.cut.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * B站投稿配置
 */
@Component
@ConfigurationProperties(prefix = "bilibili.submission")
public class BilibiliSubmissionConfig {
    
    /**
     * 单次投稿最大分P数量
     * B站限制大约在100左右，这里保守设置为100
     */
    private int maxPartsPerSubmission = 100;
    
    /**
     * 总分P数量上限
     * 超过此数量将拒绝投稿
     */
    private int maxTotalParts = 200;
    
    /**
     * 分批投稿间隔时间（毫秒）
     * 避免请求过快被限制
     */
    private long batchIntervalMs = 2000;
    
    /**
     * 是否启用分批投稿
     */
    private boolean enableBatchSubmission = true;
    
    public int getMaxPartsPerSubmission() {
        return maxPartsPerSubmission;
    }
    
    public void setMaxPartsPerSubmission(int maxPartsPerSubmission) {
        this.maxPartsPerSubmission = maxPartsPerSubmission;
    }
    
    public int getMaxTotalParts() {
        return maxTotalParts;
    }
    
    public void setMaxTotalParts(int maxTotalParts) {
        this.maxTotalParts = maxTotalParts;
    }
    
    public long getBatchIntervalMs() {
        return batchIntervalMs;
    }
    
    public void setBatchIntervalMs(long batchIntervalMs) {
        this.batchIntervalMs = batchIntervalMs;
    }
    
    public boolean isEnableBatchSubmission() {
        return enableBatchSubmission;
    }
    
    public void setEnableBatchSubmission(boolean enableBatchSubmission) {
        this.enableBatchSubmission = enableBatchSubmission;
    }
}