package com.tbw.cut.bilibili.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.BilibiliApiResponseParser;
import com.tbw.cut.bilibili.constant.BilibiliApiConstants;
import com.tbw.cut.bilibili.service.BilibiliLiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Bilibili直播相关服务实现
 */
@Slf4j
@Service
public class BilibiliLiveServiceImpl implements BilibiliLiveService {
    
    @Autowired
    private BilibiliApiClient apiClient;
    
    @Autowired
    private BilibiliApiResponseParser responseParser;
    
    @Value("${bilibili.api.base-url:https://api.bilibili.com}")
    private String baseUrl;
    
    @Override
    public JSONObject getLiveStatus(String roomId) {
        try {
            String url = baseUrl + BilibiliApiConstants.LIVE_ROOM_INFO;
            Map<String, String> params = new HashMap<>();
            params.put("room_id", roomId);
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取主播直播状态失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取主播直播状态失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getRoomInfo(String roomId) {
        try {
            String url = baseUrl + BilibiliApiConstants.LIVE_ROOM_GET_INFO;
            Map<String, String> params = new HashMap<>();
            params.put("room_id", roomId);
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取直播间基本信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取直播间基本信息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getRoomInfoOld(String userId) {
        try {
            String url = baseUrl + BilibiliApiConstants.LIVE_ROOM_GET_INFO_OLD;
            Map<String, String> params = new HashMap<>();
            params.put("mid", userId);
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取用户对应的直播间状态失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取用户对应的直播间状态失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getRoomInitInfo(String roomId) {
        try {
            String url = baseUrl + BilibiliApiConstants.LIVE_ROOM_INIT;
            Map<String, String> params = new HashMap<>();
            params.put("id", roomId);
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取房间页初始化信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取房间页初始化信息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getRoomBaseInfo(String... roomIds) {
        try {
            String url = baseUrl + BilibiliApiConstants.LIVE_ROOM_BASE_INFO;
            Map<String, String> params = new HashMap<>();
            params.put("req_biz", "web_room_componet");
            
            for (String roomId : roomIds) {
                if (StringUtils.hasText(roomId)) {
                    params.put("room_ids", roomId);
                }
            }
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取直播间基本信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取直播间基本信息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getLiveStatusBatch(String... userIds) {
        try {
            String url = baseUrl + BilibiliApiConstants.LIVE_ROOM_STATUS_BATCH;
            Map<String, String> params = new HashMap<>();
            
            for (String userId : userIds) {
                if (StringUtils.hasText(userId)) {
                    params.put("uids[]", userId);
                }
            }
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("批量获取直播间状态失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量获取直播间状态失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getAnchorInfo(String userId) {
        try {
            String url = baseUrl + BilibiliApiConstants.USER_INFO;
            Map<String, String> params = new HashMap<>();
            params.put("uid", userId);
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取主播信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取主播信息失败: " + e.getMessage(), e);
        }
    }
}