package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("anchor")
public class Anchor {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * Bilibili主播UID
     */
    private String uid;
    
    /**
     * 主播昵称
     */
    private String nickname;
    
    /**
     * 直播状态 0-未直播 1-直播中
     */
    private Integer liveStatus;
    
    /**
     * 上次检查时间
     */
    private LocalDateTime lastCheckTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}