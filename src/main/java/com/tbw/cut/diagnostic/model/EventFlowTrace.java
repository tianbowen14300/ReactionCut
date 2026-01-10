package com.tbw.cut.diagnostic.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件流跟踪
 */
public class EventFlowTrace {
    
    private Long taskId;
    private String traceId;
    private List<EventStep> steps;
    private List<EventBreakpoint> breakpoints;
    private EventFlowStatus status;
    private Duration totalDuration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String eventType;
    private boolean completed;
    
    public EventFlowTrace() {
        this.startTime = LocalDateTime.now();
        this.traceId = generateTraceId();
    }
    
    public EventFlowTrace(Long taskId, String eventType) {
        this();
        this.taskId = taskId;
        this.eventType = eventType;
    }
    
    private String generateTraceId() {
        return "TRACE-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public List<EventStep> getSteps() {
        return steps;
    }
    
    public void setSteps(List<EventStep> steps) {
        this.steps = steps;
    }
    
    public List<EventBreakpoint> getBreakpoints() {
        return breakpoints;
    }
    
    public void setBreakpoints(List<EventBreakpoint> breakpoints) {
        this.breakpoints = breakpoints;
    }
    
    public EventFlowStatus getStatus() {
        return status;
    }
    
    public void setStatus(EventFlowStatus status) {
        this.status = status;
    }
    
    public Duration getTotalDuration() {
        return totalDuration;
    }
    
    public void setTotalDuration(Duration totalDuration) {
        this.totalDuration = totalDuration;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.totalDuration = Duration.between(startTime, endTime);
        }
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && endTime == null) {
            setEndTime(LocalDateTime.now());
        }
    }
    
    @Override
    public String toString() {
        return "EventFlowTrace{" +
                "traceId='" + traceId + '\'' +
                ", taskId=" + taskId +
                ", eventType='" + eventType + '\'' +
                ", status=" + status +
                ", completed=" + completed +
                ", totalDuration=" + totalDuration +
                '}';
    }
}