package com.tbw.cut.bilibili.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.BilibiliApiResponseParser;
import com.tbw.cut.bilibili.constant.BilibiliApiConstants;
import com.tbw.cut.bilibili.service.BilibiliQRCodeLoginService;
import com.tbw.cut.bilibili.service.BilibiliUnifiedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Bilibili统一服务实现，整合所有Bilibili相关功能
 */
@Slf4j
@Service
public class BilibiliUnifiedServiceImpl implements BilibiliUnifiedService {
    
    @Autowired
    private BilibiliApiClient apiClient;
    
    @Autowired
    private BilibiliApiResponseParser responseParser;
    
    // 直播相关服务
    @Autowired
    private BilibiliLiveServiceImpl liveService;
    
    // 视频相关服务
    @Autowired
    private BilibiliVideoServiceImpl videoService;
    
    // 登录相关服务
    @Autowired
    private BilibiliLoginServiceImpl loginService;
    
    // 二维码登录服务
    @Autowired
    private BilibiliQRCodeLoginServiceImpl qrCodeLoginService;
    
    // 直播相关方法委托
    @Override
    public JSONObject getLiveStatus(String roomId) {
        return liveService.getLiveStatus(roomId);
    }
    
    @Override
    public JSONObject getRoomInfo(String roomId) {
        return liveService.getRoomInfo(roomId);
    }
    
    @Override
    public JSONObject getRoomInfoOld(String userId) {
        return liveService.getRoomInfoOld(userId);
    }
    
    @Override
    public JSONObject getRoomInitInfo(String roomId) {
        return liveService.getRoomInitInfo(roomId);
    }
    
    @Override
    public JSONObject getRoomBaseInfo(String... roomIds) {
        return liveService.getRoomBaseInfo(roomIds);
    }
    
    @Override
    public JSONObject getLiveStatusBatch(String... userIds) {
        return liveService.getLiveStatusBatch(userIds);
    }
    
    @Override
    public JSONObject getAnchorInfo(String userId) {
        return liveService.getAnchorInfo(userId);
    }
    
    // 视频相关方法委托
    @Override
    public JSONObject getUserInfo(String userId) {
        return videoService.getUserInfo(userId);
    }
    
    @Override
    public JSONObject getVideoInfo(String bvid) {
        return videoService.getVideoInfo(bvid);
    }
    
    @Override
    public JSONObject getVideoPlayInfo(String bvid, String cid) {
        return videoService.getVideoPlayInfo(bvid, cid);
    }
    
    @Override
    public JSONObject getVideoPlayInfo(String bvid, String cid, String qn, String fnval, String fnver, String fourk) {
        return videoService.getVideoPlayInfo(bvid, cid, qn, fnval, fnver, fourk);
    }
    
    public JSONObject getVideoPlayInfoByAid(String aid, String cid, String qn, String fnval, String fnver, String fourk) {
        return videoService.getVideoPlayInfoByAid(aid, cid, qn, fnval, fnver, fourk);
    }
    
    @Override
    public JSONObject searchVideos(String keyword, int page, int pageSize) {
        return videoService.searchVideos(keyword, page, pageSize);
    }
    
    @Override
    public JSONObject getVideoComments(String oid, int type, int page, int pageSize) {
        return videoService.getVideoComments(oid, type, page, pageSize);
    }
    
    @Override
    public JSONObject getVideoDetail(Long aid, String bvid) {
        return videoService.getVideoDetail(aid, bvid);
    }
    
    @Override
    public JSONObject getVideoDetailInfo(Long aid, String bvid, Integer needElec) {
        return videoService.getVideoDetailInfo(aid, bvid, needElec);
    }
    
    @Override
    public String getVideoDescription(Long aid, String bvid) {
        return videoService.getVideoDescription(aid, bvid);
    }
    
    @Override
    public JSONObject getVideoPageList(Long aid, String bvid) {
        return videoService.getVideoPageList(aid, bvid);
    }
    
    // 登录相关方法委托
    @Override
    public JSONObject getWebLoginKey() {
        return loginService.getWebLoginKey();
    }
    
    @Override
    public JSONObject webLogin(String username, String password, String token, String challenge, 
                              String validate, String seccode, int keep, String source, String goUrl) {
        return loginService.webLogin(username, password, token, challenge, validate, seccode, keep, source, goUrl);
    }
    
    // 二维码登录相关方法委托
    @Override
    public JSONObject generateQRCode() {
        return qrCodeLoginService.generateQRCode();
    }
    
    @Override
    public JSONObject pollQRCodeLogin(String qrcodeKey) {
        // 修复：调用正确的pollQRCodeStatus方法
        BilibiliQRCodeLoginService.PollResult pollResult = qrCodeLoginService.pollQRCodeStatus(qrcodeKey);
        
        // 将PollResult转换为JSONObject以保持接口一致性
        JSONObject result = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("code", pollResult.getCode());
        data.put("message", pollResult.getMessage());
        data.put("data", pollResult.getData());
        result.put("data", data);
        return result;
    }
    
    @Override
    public JSONObject customApiCall(String endpoint, Map<String, String> params, Map<String, String> headers) {
        try {
            String response = apiClient.get(endpoint, params, headers);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("自定义API调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("自定义API调用失败: " + e.getMessage(), e);
        }
    }
}