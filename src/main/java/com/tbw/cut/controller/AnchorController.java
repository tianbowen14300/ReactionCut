package com.tbw.cut.controller;

import com.tbw.cut.dto.AnchorSubscribeDTO;
import com.tbw.cut.dto.ResponseResult;
import com.tbw.cut.entity.Anchor;
import com.tbw.cut.service.AnchorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/anchor")
public class AnchorController {
    
    @Autowired
    private AnchorService anchorService;
    
    /**
     * 批量订阅主播
     */
    @PostMapping("/subscribe")
    public ResponseResult<Boolean> subscribeAnchors(@RequestBody AnchorSubscribeDTO dto) {
        try {
            boolean result = anchorService.subscribeAnchors(dto);
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("订阅主播失败", e);
            return ResponseResult.error("订阅主播失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有订阅的主播
     */
    @GetMapping("/list")
    public ResponseResult<List<Anchor>> getAllAnchors() {
        try {
            List<Anchor> anchors = anchorService.getAllSubscribedAnchors();
            return ResponseResult.success(anchors);
        } catch (Exception e) {
            log.error("获取主播列表失败", e);
            return ResponseResult.error("获取主播列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动检查主播直播状态
     */
    @PostMapping("/check")
    public ResponseResult<Boolean> checkLiveStatus() {
        try {
            anchorService.checkLiveStatus();
            return ResponseResult.success(true);
        } catch (Exception e) {
            log.error("检查主播直播状态失败", e);
            return ResponseResult.error("检查主播直播状态失败: " + e.getMessage());
        }
    }
}