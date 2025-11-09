//package com.tbw.cut.controller;
//
//import com.alibaba.fastjson.JSONObject;
//import com.tbw.cut.service.BilibiliCollectionAndPartitionService;
//import com.tbw.cut.service.BilibiliSeasonService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/bilibili/season")
//public class BilibiliSeasonController {
//
//    @Autowired
//    private BilibiliCollectionAndPartitionService seasonService;
//
//    /**
//     * 获取用户合集列表
//     * @param userId 用户ID
//     * @return 合集列表
//     */
//    @GetMapping("/user/{userId}")
//    public JSONObject getUserSeasons(@PathVariable Long userId) {
//        try {
//            log.info("获取用户合集列表，用户ID: {}", userId);
//            return seasonService.getUserCollections(userId);
//        } catch (Exception e) {
//            log.error("获取用户合集列表失败，用户ID: {}", userId, e);
//            JSONObject errorResponse = new JSONObject();
//            errorResponse.put("code", -1);
//            errorResponse.put("message", "获取用户合集列表失败: " + e.getMessage());
//            return errorResponse;
//        }
//    }
//
//    /**
//     * 获取合集章节信息
//     * @param seasonId 合集ID
//     * @return 合集章节信息
//     */
//    @GetMapping("/sections/{seasonId}")
//    public JSONObject getSeasonSections(@PathVariable Long seasonId) {
//        try {
//            log.info("获取合集章节信息，合集ID: {}", seasonId);
//            return seasonService.getSeasonSections(seasonId);
//        } catch (Exception e) {
//            log.error("获取合集章节信息失败，合集ID: {}", seasonId, e);
//            JSONObject errorResponse = new JSONObject();
//            errorResponse.put("code", -1);
//            errorResponse.put("message", "获取合集章节信息失败: " + e.getMessage());
//            return errorResponse;
//        }
//    }
//}