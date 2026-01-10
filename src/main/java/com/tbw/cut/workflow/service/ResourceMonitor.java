package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.SystemResourceInfo;
import com.tbw.cut.workflow.model.ResourceThreshold;

/**
 * 系统资源监控服务接口
 * 
 * 负责监控系统资源使用情况，包括CPU、内存、磁盘空间等，
 * 为工作流引擎提供资源状态信息以支持负载均衡决策。
 */
public interface ResourceMonitor {
    
    /**
     * 获取当前系统资源信息
     * 
     * @return 系统资源信息
     */
    SystemResourceInfo getCurrentResourceInfo();
    
    /**
     * 检查系统资源是否可用于新的工作流实例
     * 
     * @return true 如果资源充足，false 如果资源不足
     */
    boolean isResourceAvailable();
    
    /**
     * 检查系统是否处于高负载状态
     * 
     * @return true 如果系统负载过高，false 如果负载正常
     */
    boolean isHighLoad();
    
    /**
     * 获取建议的并发工作流数量
     * 基于当前系统资源状况计算最优的并发数量
     * 
     * @return 建议的并发工作流数量
     */
    int getRecommendedConcurrency();
    
    /**
     * 设置资源阈值配置
     * 
     * @param threshold 资源阈值配置
     */
    void setResourceThreshold(ResourceThreshold threshold);
    
    /**
     * 获取当前资源阈值配置
     * 
     * @return 资源阈值配置
     */
    ResourceThreshold getResourceThreshold();
    
    /**
     * 启动资源监控
     * 开始定期收集系统资源信息
     */
    void startMonitoring();
    
    /**
     * 停止资源监控
     */
    void stopMonitoring();
    
    /**
     * 获取资源监控历史数据
     * 
     * @param minutes 获取最近多少分钟的数据
     * @return 资源监控历史数据列表
     */
    java.util.List<SystemResourceInfo> getResourceHistory(int minutes);
    
    /**
     * 注册资源状态变化监听器
     * 
     * @param listener 资源状态变化监听器
     */
    void addResourceChangeListener(ResourceChangeListener listener);
    
    /**
     * 移除资源状态变化监听器
     * 
     * @param listener 资源状态变化监听器
     */
    void removeResourceChangeListener(ResourceChangeListener listener);
    
    /**
     * 资源状态变化监听器接口
     */
    interface ResourceChangeListener {
        /**
         * 当资源状态发生变化时调用
         * 
         * @param oldInfo 旧的资源信息
         * @param newInfo 新的资源信息
         */
        void onResourceChanged(SystemResourceInfo oldInfo, SystemResourceInfo newInfo);
        
        /**
         * 当系统进入高负载状态时调用
         * 
         * @param resourceInfo 当前资源信息
         */
        void onHighLoadDetected(SystemResourceInfo resourceInfo);
        
        /**
         * 当系统从高负载状态恢复时调用
         * 
         * @param resourceInfo 当前资源信息
         */
        void onLoadRecovered(SystemResourceInfo resourceInfo);
    }
}