package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 验证结果
 */
public class ValidationResult {
    
    private String validationId;
    private boolean isValid;
    private LocalDateTime validationTime;
    private String validationType;
    private String description;
    private List<String> validationErrors;
    private List<String> warnings;
    private int totalItems;
    private int validItems;
    private int invalidItems;
    private Double validationScore;
    private Long executionTimeMs;
    
    public ValidationResult() {
        this.validationTime = LocalDateTime.now();
        this.validationId = generateValidationId();
    }
    
    public ValidationResult(String validationType, boolean isValid) {
        this();
        this.validationType = validationType;
        this.isValid = isValid;
    }
    
    private String generateValidationId() {
        return "VALIDATION-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getValidationId() {
        return validationId;
    }
    
    public void setValidationId(String validationId) {
        this.validationId = validationId;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public LocalDateTime getValidationTime() {
        return validationTime;
    }
    
    public void setValidationTime(LocalDateTime validationTime) {
        this.validationTime = validationTime;
    }
    
    public String getValidationType() {
        return validationType;
    }
    
    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public int getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
    
    public int getValidItems() {
        return validItems;
    }
    
    public void setValidItems(int validItems) {
        this.validItems = validItems;
    }
    
    public int getInvalidItems() {
        return invalidItems;
    }
    
    public void setInvalidItems(int invalidItems) {
        this.invalidItems = invalidItems;
    }
    
    public Double getValidationScore() {
        return validationScore;
    }
    
    public void setValidationScore(Double validationScore) {
        this.validationScore = validationScore;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "validationId='" + validationId + '\'' +
                ", isValid=" + isValid +
                ", validationType='" + validationType + '\'' +
                ", totalItems=" + totalItems +
                ", validItems=" + validItems +
                ", invalidItems=" + invalidItems +
                ", validationScore=" + validationScore +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}