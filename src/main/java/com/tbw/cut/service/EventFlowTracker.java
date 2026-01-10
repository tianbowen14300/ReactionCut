package com.tbw.cut.service;

import com.tbw.cut.diagnostic.model.EventStep;
import com.tbw.cut.diagnostic.model.EventBreakpoint;
import java.util.List;

/**
 * 事件流追踪器接口
 * 用于追踪事件在系统中的传播路径
 */
public interface EventFlowTracker {
    
    /**
     * 开始追踪特定任务的事件流
     * 
     * @param taskId 任务ID
     */
    void startTracking(Long taskId);
    
    /**
     * 记录事件处理步骤
     * 
     * @param taskId 任务ID
     * @param step 步骤名称
     * @param data 步骤数据
     */
    void recordEventStep(Long taskId, String step, Object data);
    
    /**
     * 获取事件流历史
     * 
     * @param taskId 任务ID
     * @return 事件步骤列表
     */
    List<EventStep> getEventHistory(Long taskId);
    
    /**
     * 检测事件流中断点
     * 
     * @param taskId 任务ID
     * @return 中断点列表
     */
    List<EventBreakpoint> detectBreakpoints(Long taskId);
    
    /**
     * 停止追踪特定任务的事件流
     * 
     * @param taskId 任务ID
     */
    void stopTracking(Long taskId);
    
    /**
     * 清理过期的追踪数据
     * 
     * @param retentionHours 保留小时数
     */
    void cleanupExpiredTraces(int retentionHours);
    
    /**
     * 检查任务是否正在被追踪
     * 
     * @param taskId 任务ID
     * @return 是否正在追踪
     */
    boolean isTracking(Long taskId);
}