//package com.tbw.cut.service.impl;
//
//import com.alibaba.fastjson.JSONObject;
//import com.tbw.cut.bilibili.BilibiliApiClient;
//import com.tbw.cut.service.BilibiliSeasonService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//public class BilibiliSeasonServiceImpl implements BilibiliSeasonService {
//
//    @Autowired
//    private BilibiliApiClient apiClient;
//
//    @Override
//    public JSONObject getUserSeasons(Long userId) {
//        try {
//            log.info("获取用户合集列表，用户ID: {}", userId);
//            String response = apiClient.getUserSeasons(userId);
//            return JSONObject.parseObject(response);
//        } catch (Exception e) {
//            log.error("获取用户合集列表失败，用户ID: {}", userId, e);
//            throw new RuntimeException("获取用户合集列表失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public JSONObject getSeasonSections(Long seasonId) {
//        try {
//            log.info("获取合集章节信息，合集ID: {}", seasonId);
//            String response = apiClient.getSeasonSections(seasonId);
//            return JSONObject.parseObject(response);
//        } catch (Exception e) {
//            log.error("获取合集章节信息失败，合集ID: {}", seasonId, e);
//            throw new RuntimeException("获取合集章节信息失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public JSONObject getVideoPartitions() {
//        try {
//            log.info("获取视频分区列表");
//            String response = apiClient.getVideoPartitions();
//            return JSONObject.parseObject(response);
//        } catch (Exception e) {
//            log.error("获取视频分区列表失败", e);
//            throw new RuntimeException("获取视频分区列表失败: " + e.getMessage(), e);
//        }
//    }
//}