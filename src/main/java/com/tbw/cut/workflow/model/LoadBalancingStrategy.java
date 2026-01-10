package com.tbw.cut.workflow.model;

/**
 * 负载均衡策略枚举
 * 
 * 定义不同的负载均衡策略，用于控制工作流实例的资源分配和执行优先级
 */
public enum LoadBalancingStrategy {
    
    /**
     * 轮询策略
     * 按照工作流创建的顺序依次分配资源
     */
    ROUND_ROBIN("轮询策略", "按创建顺序依次分配资源"),
    
    /**
     * 最少连接策略
     * 优先分配给当前资源占用最少的工作流
     */
    LEAST_CONNECTIONS("最少连接策略", "优先分配给资源占用最少的工作流"),
    
    /**
     * 加权轮询策略
     * 根据工作流的优先级和资源需求进行加权分配
     */
    WEIGHTED_ROUND_ROBIN("加权轮询策略", "根据优先级和资源需求进行加权分配"),
    
    /**
     * 资源优先策略
     * 优先保证高优先级工作流的资源分配
     */
    RESOURCE_PRIORITY("资源优先策略", "优先保证高优先级工作流的资源"),
    
    /**
     * 公平分享策略
     * 尽量平均分配系统资源给所有工作流
     */
    FAIR_SHARE("公平分享策略", "平均分配系统资源给所有工作流"),
    
    /**
     * 自适应策略
     * 根据系统当前负载情况自动选择最优策略
     */
    ADAPTIVE("自适应策略", "根据系统负载自动选择最优策略");
    
    private final String displayName;
    private final String description;
    
    LoadBalancingStrategy(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取默认的负载均衡策略
     * 
     * @return 默认策略
     */
    public static LoadBalancingStrategy getDefault() {
        return FAIR_SHARE;
    }
    
    /**
     * 根据系统负载情况推荐合适的策略
     * 
     * @param resourcePressureScore 资源压力分数（0-100）
     * @param activeWorkflowCount 活跃工作流数量
     * @return 推荐的负载均衡策略
     */
    public static LoadBalancingStrategy recommend(double resourcePressureScore, int activeWorkflowCount) {
        if (resourcePressureScore < 30) {
            // 低负载：使用公平分享策略
            return FAIR_SHARE;
        } else if (resourcePressureScore < 60) {
            // 中等负载：使用轮询策略
            return ROUND_ROBIN;
        } else if (resourcePressureScore < 80) {
            // 高负载：使用资源优先策略
            return RESOURCE_PRIORITY;
        } else {
            // 极高负载：使用最少连接策略
            return LEAST_CONNECTIONS;
        }
    }
    
    /**
     * 检查策略是否适合当前系统状况
     * 
     * @param resourcePressureScore 资源压力分数
     * @param activeWorkflowCount 活跃工作流数量
     * @return true 如果策略适合，false 如果不适合
     */
    public boolean isSuitableFor(double resourcePressureScore, int activeWorkflowCount) {
        switch (this) {
            case ROUND_ROBIN:
                return resourcePressureScore < 70 && activeWorkflowCount <= 10;
            case LEAST_CONNECTIONS:
                return resourcePressureScore > 60;
            case WEIGHTED_ROUND_ROBIN:
                return activeWorkflowCount > 3 && resourcePressureScore < 80;
            case RESOURCE_PRIORITY:
                return resourcePressureScore > 50;
            case FAIR_SHARE:
                return resourcePressureScore < 60 && activeWorkflowCount <= 15;
            case ADAPTIVE:
                return true; // 自适应策略总是适合
            default:
                return true;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}