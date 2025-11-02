package com.tbw.cut.bilibili.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliService;
import com.tbw.cut.bilibili.service.impl.BilibiliUnifiedServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Bilibili服务实现，保持向后兼容
 * 
 * @deprecated 请使用专门的服务实现：
 * - {@link com.tbw.cut.bilibili.service.impl.BilibiliLiveServiceImpl} 直播相关功能
 * - {@link com.tbw.cut.bilibili.service.impl.BilibiliVideoServiceImpl} 视频相关功能
 * - {@link com.tbw.cut.bilibili.service.impl.BilibiliLoginServiceImpl} 登录相关功能
 * - {@link com.tbw.cut.bilibili.service.impl.BilibiliUnifiedServiceImpl} 统一服务实现
 */
@Slf4j
@Service
@Deprecated
public class BilibiliServiceImpl implements BilibiliService {
    
    @Autowired
    private BilibiliUnifiedServiceImpl unifiedService;
    
    @Override
    public JSONObject getUserInfo(String userId) {
        return unifiedService.getUserInfo(userId);
    }
    
    @Override
    public JSONObject getVideoInfo(String bvid) {
        return unifiedService.getVideoInfo(bvid);
    }
    
    @Override
    public JSONObject getVideoPlayInfo(String bvid, String cid) {
        return unifiedService.getVideoPlayInfo(bvid, cid);
    }
    
    @Override
    public JSONObject getVideoPlayInfo(String bvid, String cid, String qn, String fnval, String fnver, String fourk) {
        return unifiedService.getVideoPlayInfo(bvid, cid, qn, fnval, fnver, fourk);
    }
    
    public JSONObject getVideoPlayInfoByAid(String aid, String cid, String qn, String fnval, String fnver, String fourk) {
        return unifiedService.getVideoPlayInfoByAid(aid, cid, qn, fnval, fnver, fourk);
    }
    
    @Override
    public JSONObject getLiveStatus(String roomId) {
        return unifiedService.getLiveStatus(roomId);
    }
    
    @Override
    public JSONObject getAnchorInfo(String userId) {
        return unifiedService.getAnchorInfo(userId);
    }
    
    @Override
    public JSONObject searchVideos(String keyword, int page, int pageSize) {
        return unifiedService.searchVideos(keyword, page, pageSize);
    }
    
    @Override
    public JSONObject getVideoComments(String oid, int type, int page, int pageSize) {
        return unifiedService.getVideoComments(oid, type, page, pageSize);
    }
    
    @Override
    public JSONObject getRoomInfo(String roomId) {
        return unifiedService.getRoomInfo(roomId);
    }
    
    @Override
    public JSONObject getRoomInfoOld(String userId) {
        return unifiedService.getRoomInfoOld(userId);
    }
    
    @Override
    public JSONObject getRoomInitInfo(String roomId) {
        return unifiedService.getRoomInitInfo(roomId);
    }
    
    @Override
    public JSONObject getRoomBaseInfo(String... roomIds) {
        return unifiedService.getRoomBaseInfo(roomIds);
    }
    
    @Override
    public JSONObject getLiveStatusBatch(String... userIds) {
        return unifiedService.getLiveStatusBatch(userIds);
    }
    
    @Override
    public JSONObject getWebLoginKey() {
        return unifiedService.getWebLoginKey();
    }
    
    @Override
    public JSONObject webLogin(String username, String password, String token, String challenge, 
                              String validate, String seccode, int keep, String source, String goUrl) {
        return unifiedService.webLogin(username, password, token, challenge, validate, seccode, keep, source, goUrl);
    }
    
    @Override
    public JSONObject getVideoDetail(Long aid, String bvid) {
        return unifiedService.getVideoDetail(aid, bvid);
    }
    
    @Override
    public JSONObject getVideoDetailInfo(Long aid, String bvid, Integer needElec) {
        return unifiedService.getVideoDetailInfo(aid, bvid, needElec);
    }
    
    @Override
    public String getVideoDescription(Long aid, String bvid) {
        return unifiedService.getVideoDescription(aid, bvid);
    }
    
    @Override
    public JSONObject getVideoPageList(Long aid, String bvid) {
        return unifiedService.getVideoPageList(aid, bvid);
    }
    
    @Override
    public JSONObject generateQRCode() {
        return unifiedService.generateQRCode();
    }
    
    @Override
    public JSONObject pollQRCodeLogin(String qrcodeKey) {
        return unifiedService.pollQRCodeLogin(qrcodeKey);
    }
    
    @Override
    public JSONObject customApiCall(String endpoint, Map<String, String> params, Map<String, String> headers) {
        return unifiedService.customApiCall(endpoint, params, headers);
    }
}