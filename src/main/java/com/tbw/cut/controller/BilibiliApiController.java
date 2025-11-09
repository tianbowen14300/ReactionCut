package com.tbw.cut.controller;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.service.BilibiliSeasonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/bilibili")
public class BilibiliApiController {
    
    @Autowired
    private BilibiliSeasonService seasonService;
    
    /**
     * 获取用户合集列表
     * @param mid 用户ID
     * @return 合集列表
     */
    @GetMapping("/collections")
    public JSONObject getUserCollections(@RequestParam Long mid) {
        try {
            log.info("获取用户合集列表，用户ID: {}", mid);
            JSONObject response = seasonService.getUserSeasons(mid);
            
            JSONObject result = new JSONObject();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", response);
            return result;
        } catch (Exception e) {
            log.error("获取用户合集列表失败，用户ID: {}", mid, e);
            JSONObject result = new JSONObject();
            result.put("code", -1);
            result.put("message", "获取用户合集列表失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 获取所有视频分区
     * @return 分区列表
     */
    @GetMapping("/partitions")
    public JSONObject getAllPartitions() {
        try {
            log.info("获取视频分区列表");
            JSONObject response = seasonService.getVideoPartitions();
            
            JSONObject result = new JSONObject();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", response);
            return result;
        } catch (Exception e) {
            log.error("获取视频分区列表失败", e);
            JSONObject result = new JSONObject();
            result.put("code", -1);
            result.put("message", "获取视频分区列表失败: " + e.getMessage());
            return result;
        }
    }
}