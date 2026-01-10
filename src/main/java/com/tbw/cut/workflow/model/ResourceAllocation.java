package com.tbw.cut.workflow.model;

import java.time.LocalDateTime;

/**
 * 资源分配模型
 * 
 * 表示为工作流实例分配的系统资源，包括CPU、内存、磁盘空间等
 */
public class ResourceAllocation {
    
    private String workflowInstanceId;
    private String taskId;
    private int allocatedCpuCores;
    private long allocatedMemoryMB;
    private long allocatedDiskSpaceGB;
    private int allocatedThreads;
    private int priority;
    private ResourceAllocationStatus status;
    private LocalDateTime allocationTime;
    private LocalDateTime releaseTime;
    private String allocationReason;
    
    // 构造函数
    public ResourceAllocation() {
        this.allocationTime = LocalDateTime.now();
        this.status = ResourceAllocationStatus.ALLOCATED;
    }
    
    public ResourceAllocation(String workflowInstanceId, String taskId) {
        this();
        this.workflowInstanceId = workflowInstanceId;
        this.taskId = taskId;
    }
    
    public ResourceAllocation(String workflowInstanceId, String taskId,
                            int allocatedCpuCores, long allocatedMemoryMB,
                            long allocatedDiskSpaceGB, int allocatedThreads,
                            int priority) {
        this(workflowInstanceId, taskId);
        this.allocatedCpuCores = allocatedCpuCores;
        this.allocatedMemoryMB = allocatedMemoryMB;
        this.allocatedDiskSpaceGB = allocatedDiskSpaceGB;
        this.allocatedThreads = allocatedThreads;
        this.priority = priority;
    }
    
    // Getters and Setters
    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }
    
    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public int getAllocatedCpuCores() {
        return allocatedCpuCores;
    }
    
    public void setAllocatedCpuCores(int allocatedCpuCores) {
        this.allocatedCpuCores = allocatedCpuCores;
    }
    
    public long getAllocatedMemoryMB() {
        return allocatedMemoryMB;
    }
    
    public void setAllocatedMemoryMB(long allocatedMemoryMB) {
        this.allocatedMemoryMB = allocatedMemoryMB;
    }
    
    public long getAllocatedDiskSpaceGB() {
        return allocatedDiskSpaceGB;
    }
    
    public void setAllocatedDiskSpaceGB(long allocatedDiskSpaceGB) {
        this.allocatedDiskSpaceGB = allocatedDiskSpaceGB;
    }
    
    public int getAllocatedThreads() {
        return allocatedThreads;
    }
    
    public void setAllocatedThreads(int allocatedThreads) {
        this.allocatedThreads = allocatedThreads;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public ResourceAllocationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ResourceAllocationStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getAllocationTime() {
        return allocationTime;
    }
    
    public void setAllocationTime(LocalDateTime allocationTime) {
        this.allocationTime = allocationTime;
    }
    
    public LocalDateTime getReleaseTime() {
        return releaseTime;
    }
    
    public void setReleaseTime(LocalDateTime releaseTime) {
        this.releaseTime = releaseTime;
    }
    
    public String getAllocationReason() {
        return allocationReason;
    }
    
    public void setAllocationReason(String allocationReason) {
        this.allocationReason = allocationReason;
    }
    
    /**
     * 计算资源分配的权重分数
     * 用于负载均衡决策
     * 
     * @return 权重分数
     */
    public double getWeightScore() {
        // 基于分配的资源量和优先级计算权重
        double resourceWeight = (allocatedCpuCores * 0.3) + 
                               (allocatedMemoryMB / 1024.0 * 0.3) + 
                               (allocatedDiskSpaceGB * 0.2) + 
                               (allocatedThreads * 0.2);
        
        double priorityWeight = priority * 0.1;
        
        return resourceWeight + priorityWeight;
    }
    
    /**
     * 检查资源分配是否有效
     * 
     * @return true 如果分配有效，false 如果无效
     */
    public boolean isValid() {
        return workflowInstanceId != null && !workflowInstanceId.isEmpty() &&
               taskId != null && !taskId.isEmpty() &&
               allocatedCpuCores >= 0 && allocatedMemoryMB >= 0 &&
               allocatedDiskSpaceGB >= 0 && allocatedThreads >= 0 &&
               priority >= 0 && status != null;
    }
    
    /**
     * 释放资源分配
     */
    public void release() {
        this.status = ResourceAllocationStatus.RELEASED;
        this.releaseTime = LocalDateTime.now();
    }
    
    /**
     * 暂停资源分配
     */
    public void suspend() {
        this.status = ResourceAllocationStatus.SUSPENDED;
    }
    
    /**
     * 恢复资源分配
     */
    public void resume() {
        this.status = ResourceAllocationStatus.ALLOCATED;
    }
    
    /**
     * 创建默认的资源分配
     * 
     * @param workflowInstanceId 工作流实例ID
     * @param taskId 任务ID
     * @return 默认资源分配
     */
    public static ResourceAllocation createDefault(String workflowInstanceId, String taskId) {
        return new ResourceAllocation(
            workflowInstanceId, taskId,
            2, // 2个CPU核心
            2048, // 2GB内存
            10, // 10GB磁盘空间
            10, // 10个线程
            5 // 中等优先级
        );
    }
    
    /**
     * 创建轻量级资源分配
     * 
     * @param workflowInstanceId 工作流实例ID
     * @param taskId 任务ID
     * @return 轻量级资源分配
     */
    public static ResourceAllocation createLightweight(String workflowInstanceId, String taskId) {
        return new ResourceAllocation(
            workflowInstanceId, taskId,
            1, // 1个CPU核心
            1024, // 1GB内存
            5, // 5GB磁盘空间
            5, // 5个线程
            3 // 低优先级
        );
    }
    
    /**
     * 创建高性能资源分配
     * 
     * @param workflowInstanceId 工作流实例ID
     * @param taskId 任务ID
     * @return 高性能资源分配
     */
    public static ResourceAllocation createHighPerformance(String workflowInstanceId, String taskId) {
        return new ResourceAllocation(
            workflowInstanceId, taskId,
            4, // 4个CPU核心
            4096, // 4GB内存
            20, // 20GB磁盘空间
            20, // 20个线程
            8 // 高优先级
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "ResourceAllocation{workflowId='%s', taskId='%s', CPU=%d, Memory=%dMB, " +
            "Disk=%dGB, Threads=%d, Priority=%d, Status=%s, AllocatedAt=%s}",
            workflowInstanceId, taskId, allocatedCpuCores, allocatedMemoryMB,
            allocatedDiskSpaceGB, allocatedThreads, priority, status, allocationTime
        );
    }
    
    /**
     * 资源分配状态枚举
     */
    public enum ResourceAllocationStatus {
        ALLOCATED("已分配"),
        SUSPENDED("已暂停"),
        RELEASED("已释放"),
        EXPIRED("已过期");
        
        private final String description;
        
        ResourceAllocationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
}