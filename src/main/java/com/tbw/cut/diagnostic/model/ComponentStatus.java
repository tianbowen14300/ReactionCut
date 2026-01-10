package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 组件状态信息
 */
public class ComponentStatus {
    
    private String componentName;
    private ComponentType componentType;
    private HealthStatus status;
    private String statusMessage;
    private LocalDateTime lastCheckTime;
    private Map<String, Object> metrics;
    private String version;
    private Long responseTimeMs;
    
    public ComponentStatus() {
        this.lastCheckTime = LocalDateTime.now();
    }
    
    public ComponentStatus(String componentName, ComponentType componentType, HealthStatus status) {
        this();
        this.componentName = componentName;
        this.componentType = componentType;
        this.status = status;
    }
    
    // Getters and Setters
    public String getComponentName() {
        return componentName;
    }
    
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
    
    public ComponentType getComponentType() {
        return componentType;
    }
    
    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }
    
    public HealthStatus getStatus() {
        return status;
    }
    
    public void setStatus(HealthStatus status) {
        this.status = status;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }
    
    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }
    
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    @Override
    public String toString() {
        return "ComponentStatus{" +
                "componentName='" + componentName + '\'' +
                ", componentType=" + componentType +
                ", status=" + status +
                ", statusMessage='" + statusMessage + '\'' +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
}