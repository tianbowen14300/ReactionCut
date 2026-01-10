package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事件处理步骤
 */
public class EventStep {
    
    private String stepId;
    private String stepName;
    private String componentName;
    private LocalDateTime timestamp;
    private EventStepType stepType;
    private EventStepStatus status;
    private String description;
    private Map<String, Object> data;
    private String errorMessage;
    private Long executionTimeMs;
    private String nextStep;
    
    public EventStep() {
        this.timestamp = LocalDateTime.now();
        this.stepId = generateStepId();
    }
    
    public EventStep(String stepName, String componentName, EventStepType stepType) {
        this();
        this.stepName = stepName;
        this.componentName = componentName;
        this.stepType = stepType;
    }
    
    private String generateStepId() {
        return "STEP-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getStepId() {
        return stepId;
    }
    
    public void setStepId(String stepId) {
        this.stepId = stepId;
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
    
    public EventStepType getStepType() {
        return stepType;
    }
    
    public void setStepType(EventStepType stepType) {
        this.stepType = stepType;
    }
    
    public EventStepStatus getStatus() {
        return status;
    }
    
    public void setStatus(EventStepStatus status) {
        this.status = status;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public String getNextStep() {
        return nextStep;
    }
    
    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }
    
    @Override
    public String toString() {
        return "EventStep{" +
                "stepId='" + stepId + '\'' +
                ", stepName='" + stepName + '\'' +
                ", componentName='" + componentName + '\'' +
                ", stepType=" + stepType +
                ", status=" + status +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}