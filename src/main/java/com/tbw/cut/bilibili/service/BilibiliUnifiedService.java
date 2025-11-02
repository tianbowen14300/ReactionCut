package com.tbw.cut.bilibili.service;

import com.alibaba.fastjson.JSONObject;

/**
 * Bilibili统一服务接口，整合所有Bilibili相关功能
 */
public interface BilibiliUnifiedService extends BilibiliLiveService, BilibiliVideoService, BilibiliLoginService {
    
    // 继承所有子接口的方法，无需额外定义
    
    /**
     * 自定义API调用
     * @param endpoint API端点
     * @param params 请求参数
     * @param headers 请求头
     * @return 响应数据
     */
    JSONObject customApiCall(String endpoint, java.util.Map<String, String> params, java.util.Map<String, String> headers);
    
    // 从BilibiliVideoService继承的方法
    JSONObject getVideoPlayInfo(String bvid, String cid, String qn, String fnval, String fnver, String fourk);
    
    JSONObject getVideoPlayInfoByAid(String aid, String cid, String qn, String fnval, String fnver, String fourk);
}