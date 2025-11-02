package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_download")
public class VideoDownload {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 视频BV号
     */
    private String bvid;
    
    /**
     * 视频AV号
     */
    private String aid;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 分P标题
     */
    private String partTitle;
    
    /**
     * 分P总数
     */
    private Integer partCount;
    
    /**
     * 当前分P序号
     */
    private Integer currentPart;
    
    /**
     * 下载链接
     */
    private String downloadUrl;
    
    /**
     * 本地存储路径
     */
    private String localPath;
    
    /**
     * 分辨率
     */
    private String resolution;
    
    /**
     * 编码格式
     */
    private String codec;
    
    /**
     * 流媒体格式
     */
    private String format;
    
    /**
     * 下载状态 0-待下载 1-下载中 2-下载完成 3-下载失败
     */
    private Integer status;
    
    /**
     * 下载进度百分比
     */
    private Integer progress;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}