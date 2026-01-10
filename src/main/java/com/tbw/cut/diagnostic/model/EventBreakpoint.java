package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;

/**
 * 事件流中断点
 */
public class EventBreakpoint {
    
    private String breakpointId;
    private String stepName;
    private String componentName;
    private LocalDateTime timestamp;
    private BreakpointType type;
    private String reason;
    private String description;
    private String expectedNextStep;
    private String actualNextStep;
    private Long timeoutMs;
    
    public EventBreakpoint() {
        this.timestamp = LocalDateTime.now();
        this.breakpointId = generateBreakpointId();
    }
    
    public EventBreakpoint(String stepName, String componentName, BreakpointType type, String reason) {
        this();
        this.stepName = stepName;
        this.componentName = componentName;
        this.type = type;
        this.reason = reason;
    }
    
    private String generateBreakpointId() {
        return "BP-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getBreakpointId() {
        return breakpointId;
    }
    
    public void setBreakpointId(String breakpointId) {
        this.breakpointId = breakpointId;
    }
    
    public String getStepName() {
        return stepName;
    }
    
    public void setStepName(String stepName) {
        this.stepName = stepName;
    }
    
    public String getComponentName() {
        return componentName;
    }
    
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public BreakpointType getType() {
        return type;
    }
    
    public void setType(BreakpointType type) {
        this.type = type;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getExpectedNextStep() {
        return expectedNextStep;
    }
    
    public void setExpectedNextStep(String expectedNextStep) {
        this.expectedNextStep = expectedNextStep;
    }
    
    public String getActualNextStep() {
        return actualNextStep;
    }
    
    public void setActualNextStep(String actualNextStep) {
        this.actualNextStep = actualNextStep;
    }
    
    public Long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    @Override
    public String toString() {
        return "EventBreakpoint{" +
                "breakpointId='" + breakpointId + '\'' +
                ", stepName='" + stepName + '\'' +
                ", componentName='" + componentName + '\'' +
                ", type=" + type +
                ", reason='" + reason + '\'' +
                '}';
    }
}