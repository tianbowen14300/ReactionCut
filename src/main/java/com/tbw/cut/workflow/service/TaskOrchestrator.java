package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowStep;

/**
 * 任务编排器接口
 * 负责协调工作流中各个步骤的执行顺序和依赖关系
 */
public interface TaskOrchestrator {
    
    /**
     * 编排并执行工作流任务
     * 
     * @param instance 工作流实例
     * @throws Exception 执行过程中的异常
     */
    void orchestrateTask(WorkflowInstance instance) throws Exception;
    
    /**
     * 执行单个步骤
     * 
     * @param instance 工作流实例
     * @param step 要执行的步骤
     * @return 步骤执行结果
     * @throws Exception 执行过程中的异常
     */
    StepResult executeStep(WorkflowInstance instance, WorkflowStep step) throws Exception;
    
    /**
     * 处理步骤执行失败
     * 
     * @param instance 工作流实例
     * @param step 失败的步骤
     * @param error 错误信息
     */
    void handleStepFailure(WorkflowInstance instance, WorkflowStep step, Exception error);
    
    /**
     * 检查步骤是否可以执行
     * 
     * @param instance 工作流实例
     * @param step 要检查的步骤
     * @return 是否可以执行
     */
    boolean canExecuteStep(WorkflowInstance instance, WorkflowStep step);
    
    /**
     * 步骤执行结果
     */
    class StepResult {
        private final boolean success;
        private final String outputPath;
        private final String errorMessage;
        private final Object resultData;
        
        private StepResult(boolean success, String outputPath, String errorMessage, Object resultData) {
            this.success = success;
            this.outputPath = outputPath;
            this.errorMessage = errorMessage;
            this.resultData = resultData;
        }
        
        public static StepResult success(String outputPath) {
            return new StepResult(true, outputPath, null, null);
        }
        
        public static StepResult success(String outputPath, Object resultData) {
            return new StepResult(true, outputPath, null, resultData);
        }
        
        public static StepResult failure(String errorMessage) {
            return new StepResult(false, null, errorMessage, null);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getOutputPath() {
            return outputPath;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public Object getResultData() {
            return resultData;
        }
    }
}