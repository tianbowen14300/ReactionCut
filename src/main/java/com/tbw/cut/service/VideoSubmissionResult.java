package com.tbw.cut.service;

/**
 * 视频投稿结果
 */
public class VideoSubmissionResult {
    private String bvid;
    private Long aid;
    private boolean success;
    private String errorMessage;
    
    public VideoSubmissionResult() {
    }
    
    public VideoSubmissionResult(String bvid, Long aid, boolean success) {
        this.bvid = bvid;
        this.aid = aid;
        this.success = success;
    }
    
    public VideoSubmissionResult(String bvid, Long aid, boolean success, String errorMessage) {
        this.bvid = bvid;
        this.aid = aid;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public static VideoSubmissionResult success(String bvid, Long aid) {
        return new VideoSubmissionResult(bvid, aid, true);
    }
    
    public static VideoSubmissionResult failure(String errorMessage) {
        return new VideoSubmissionResult(null, null, false, errorMessage);
    }
    
    public String getBvid() {
        return bvid;
    }
    
    public void setBvid(String bvid) {
        this.bvid = bvid;
    }
    
    public Long getAid() {
        return aid;
    }
    
    public void setAid(Long aid) {
        this.aid = aid;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return "VideoSubmissionResult{" +
                "bvid='" + bvid + '\'' +
                ", aid=" + aid +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}