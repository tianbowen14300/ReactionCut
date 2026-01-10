package com.tbw.cut.controller;

import com.tbw.cut.service.TaskRelationDiagnosticService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务关联诊断控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostic/task-relations")
public class TaskRelationDiagnosticController {
    
    @Autowired
    private TaskRelationDiagnosticService diagnosticService;
    
    /**
     * 诊断指定下载任务的关联状态
     */
    @GetMapping("/download/{downloadTaskId}")
    public ResponseEntity<Map<String, Object>> diagnoseDownloadTask(@PathVariable Long downloadTaskId) {
        log.info("Diagnosing download task: {}", downloadTaskId);
        Map<String, Object> diagnosis = diagnosticService.diagnoseDownloadTask(downloadTaskId);
        return ResponseEntity.ok(diagnosis);
    }
    
    /**
     * 诊断指定投稿任务的关联状态
     */
    @GetMapping("/submission/{submissionTaskId}")
    public ResponseEntity<Map<String, Object>> diagnoseSubmissionTask(@PathVariable String submissionTaskId) {
        log.info("Diagnosing submission task: {}", submissionTaskId);
        Map<String, Object> diagnosis = diagnosticService.diagnoseSubmissionTask(submissionTaskId);
        return ResponseEntity.ok(diagnosis);
    }
    
    /**
     * 获取任务关联统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        log.info("Getting task relation statistics");
        Map<String, Long> stats = diagnosticService.getRelationStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 验证任务关联完整性
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateIntegrity() {
        log.info("Validating task relation integrity");
        Map<String, Object> validation = diagnosticService.validateRelationIntegrity();
        return ResponseEntity.ok(validation);
    }
    
    /**
     * 修复孤立的任务关联
     */
    @PostMapping("/repair-orphaned")
    public ResponseEntity<Map<String, Object>> repairOrphanedRelations() {
        log.info("Repairing orphaned task relations");
        int repairedCount = diagnosticService.repairOrphanedRelations();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("repairedCount", repairedCount);
        result.put("message", "Repaired " + repairedCount + " orphaned relations");
        
        return ResponseEntity.ok(result);
    }
}