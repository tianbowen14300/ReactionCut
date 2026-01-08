package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 视频分P信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPart {
    
    /**
     * 分P的CID
     */
    private Long cid;
    
    /**
     * 分P标题
     */
    private String title;
    
    /**
     * 分P时长（秒）
     */
    private Integer duration;
    
    /**
     * 分P序号
     */
    private Integer partNumber;
    
    /**
     * 视频流URL
     */
    private String streamUrl;
    
    /**
     * 预估文件大小（字节）
     */
    private Long estimatedSize;
    
    /**
     * 输出文件名
     */
    private String outputFileName;
    
    /**
     * 输出路径
     */
    private String outputPath;
    
    /**
     * 数据库记录ID（用于关联VideoDownload表记录）
     */
    private Long databaseId;
    
    /**
     * 视频BVID（用于URL刷新）
     */
    private String bvid;
    
    /**
     * 额外参数（用于存储DASH合并等特殊信息）
     */
    private Map<String, Object> extraParams;
    
    /**
     * 获取分P索引（兼容方法）
     */
    public Integer getPartIndex() {
        return partNumber;
    }
    
    /**
     * 获取URL（兼容方法）
     */
    public String getUrl() {
        return streamUrl;
    }
    
    /**
     * Builder扩展方法
     */
    public static class VideoPartBuilder {
        public VideoPartBuilder url(String url) {
            this.streamUrl = url;
            return this;
        }
        
        public VideoPartBuilder partIndex(Integer partIndex) {
            this.partNumber = partIndex;
            return this;
        }
        
        public VideoPartBuilder totalParts(int totalParts) {
            // 这个方法用于兼容，但不存储值，因为单个VideoPart不需要知道总数
            return this;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoPart videoPart = (VideoPart) o;
        return cid != null && cid.equals(videoPart.cid);
    }
    
    @Override
    public int hashCode() {
        return cid != null ? cid.hashCode() : 0;
    }
}