package com.tbw.cut.bilibili.service;

import com.alibaba.fastjson.JSONObject;

/**
 * Bilibili直播相关服务接口
 */
public interface BilibiliLiveService {
    
    /**
     * 获取主播直播状态
     * @param roomId 房间ID
     * @return 直播状态信息
     */
    JSONObject getLiveStatus(String roomId);
    
    /**
     * 获取直播间基本信息
     * @param roomId 直播间号
     * @return 直播间基本信息
     */
    JSONObject getRoomInfo(String roomId);
    
    /**
     * 获取用户对应的直播间状态
     * @param userId 用户ID
     * @return 直播间状态信息
     */
    JSONObject getRoomInfoOld(String userId);
    
    /**
     * 获取房间页初始化信息
     * @param roomId 直播间号（短号）
     * @return 房间初始化信息
     */
    JSONObject getRoomInitInfo(String roomId);
    
    /**
     * 获取直播间基本信息（新接口）
     * @param roomIds 直播间ID列表
     * @return 直播间基本信息
     */
    JSONObject getRoomBaseInfo(String... roomIds);
    
    /**
     * 批量获取直播间状态
     * @param userIds 用户ID列表
     * @return 直播间状态信息
     */
    JSONObject getLiveStatusBatch(String... userIds);
    
    /**
     * 获取主播信息
     * @param userId 主播用户ID
     * @return 主播信息
     */
    JSONObject getAnchorInfo(String userId);
}